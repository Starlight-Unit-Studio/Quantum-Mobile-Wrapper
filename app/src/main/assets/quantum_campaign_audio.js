(function () {
  'use strict';

  var PREFIX = '/assets/sounds/campaign/';
  var SETTINGS_KEY = 'stu_audio_settings_v1';
  var nmp = window.QuantumNMP;
  if (!nmp || typeof nmp.play !== 'function') return;

  function clamp(value, fallback) {
    var number = Number(value);
    if (!isFinite(number)) return fallback;
    return Math.max(0, Math.min(1, number));
  }

  function readAudioSettings() {
    try {
      if (window.STU_AUDIO && typeof window.STU_AUDIO.getSettings === 'function') {
        var settings = window.STU_AUDIO.getSettings();
        if (settings && typeof settings === 'object') {
          return {
            master: clamp(settings.master, 0.65),
            music: settings.music !== false
          };
        }
      }
    } catch (e) {}

    try {
      var raw = window.localStorage ? window.localStorage.getItem(SETTINGS_KEY) : '';
      if (raw) {
        var stored = JSON.parse(raw);
        return {
          master: clamp(stored.master, 0.65),
          music: stored.music !== false
        };
      }
    } catch (e) {}

    return { master: 0.65, music: true };
  }

  function syncNativeSettings(settings) {
    var cfg = settings && typeof settings === 'object' ? settings : readAudioSettings();
    var master = clamp(cfg.master, 0.65);
    var musicEnabled = cfg.music !== false;

    try {
      if (typeof nmp.setVolume === 'function') nmp.setVolume(master);
    } catch (e) {}

    try {
      if (typeof nmp.setEnabled === 'function') nmp.setEnabled(musicEnabled);
    } catch (e) {}

    return musicEnabled && master > 0;
  }

  function resolveCampaignSource(audio) {
    var candidates = [];
    if (audio.currentSrc) candidates.push(audio.currentSrc);
    if (audio.getAttribute('src')) candidates.push(audio.getAttribute('src'));

    var sources = audio.querySelectorAll('source[src]');
    for (var i = 0; i < sources.length; i += 1) {
      candidates.push(sources[i].getAttribute('src'));
    }

    for (var j = 0; j < candidates.length; j += 1) {
      try {
        var url = new URL(candidates[j], window.location.href);
        if (url.protocol === 'https:' && url.pathname.indexOf(PREFIX) === 0) {
          return url.href;
        }
      } catch (e) {}
    }
    return null;
  }

  function handoff(audio) {
    var source = resolveCampaignSource(audio);
    if (!source) return false;

    try {
      audio.pause();
      audio.autoplay = false;
    } catch (e) {}

    syncNativeSettings();

    if (window.__quantumNmpCampaignSource !== source) {
      window.__quantumNmpCampaignSource = source;
      nmp.play(source, !!audio.loop);
    }
    return true;
  }

  function scan() {
    var audios = document.querySelectorAll('audio');
    for (var i = 0; i < audios.length; i += 1) {
      if (handoff(audios[i])) return;
    }
  }

  syncNativeSettings();
  scan();

  if (!window.__quantumNmpAudioSettingsListener) {
    window.__quantumNmpAudioSettingsListener = function (event) {
      var detail = event && event.detail && typeof event.detail === 'object'
        ? event.detail
        : null;
      syncNativeSettings(detail);
    };
    window.addEventListener('stu:audio-settings-changed', window.__quantumNmpAudioSettingsListener);
  }

  if (!window.__quantumNmpCampaignObserver && document.documentElement) {
    window.__quantumNmpCampaignObserver = new MutationObserver(function () {
      scan();
    });
    window.__quantumNmpCampaignObserver.observe(document.documentElement, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['src', 'loop', 'autoplay']
    });
  }
})();
