(function () {
  'use strict';

  var PREFIX = '/assets/sounds/campaign/';
  var SETTINGS_KEY = 'stu_audio_settings_v1';
  var DEFAULT_MASTER = 0.65;
  var DEFAULT_MUSIC_BASE_VOLUME = 0.60;
  var nmp = window.QuantumNMP;
  if (!nmp || typeof nmp.play !== 'function') return;

  var nativeAudioState = {
    master: DEFAULT_MASTER,
    music: true,
    sfx: true,
    baseVolume: DEFAULT_MUSIC_BASE_VOLUME
  };

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
            master: clamp(settings.master, DEFAULT_MASTER),
            music: settings.music !== false,
            sfx: settings.sfx !== false
          };
        }
      }
    } catch (e) {}

    try {
      var raw = window.localStorage ? window.localStorage.getItem(SETTINGS_KEY) : '';
      if (raw) {
        var stored = JSON.parse(raw);
        return {
          master: clamp(stored.master, DEFAULT_MASTER),
          music: stored.music !== false,
          sfx: stored.sfx !== false
        };
      }
    } catch (e) {}

    return { master: DEFAULT_MASTER, music: true, sfx: true };
  }

  function resolveBaseVolume(audio) {
    if (!audio) return nativeAudioState.baseVolume;

    try {
      if (audio.dataset && audio.dataset.baseVolume != null) {
        var explicit = Number(audio.dataset.baseVolume);
        if (isFinite(explicit)) return clamp(explicit, DEFAULT_MUSIC_BASE_VOLUME);
      }
    } catch (e) {}

    try {
      if (typeof audio.__stuBaseVolume === 'number' && isFinite(audio.__stuBaseVolume)) {
        return clamp(audio.__stuBaseVolume, DEFAULT_MUSIC_BASE_VOLUME);
      }
    } catch (e) {}

    return DEFAULT_MUSIC_BASE_VOLUME;
  }

  function syncNativeSettings(settings, baseVolume) {
    var cfg = settings && typeof settings === 'object' ? settings : readAudioSettings();

    nativeAudioState.master = clamp(cfg.master, DEFAULT_MASTER);
    nativeAudioState.music = cfg.music !== false;
    nativeAudioState.sfx = cfg.sfx !== false;

    if (baseVolume != null) {
      nativeAudioState.baseVolume = clamp(baseVolume, DEFAULT_MUSIC_BASE_VOLUME);
    }

    // Keep the complete game audio state visible to the wrapper layer for diagnostics
    // and future native SFX support. Quantum NMP itself owns campaign music only.
    window.__quantumAudioState = {
      master: nativeAudioState.master,
      music: nativeAudioState.music,
      sfx: nativeAudioState.sfx,
      baseVolume: nativeAudioState.baseVolume,
      effectiveMusicVolume: clamp(
        nativeAudioState.master * nativeAudioState.baseVolume,
        DEFAULT_MASTER * DEFAULT_MUSIC_BASE_VOLUME
      )
    };

    try {
      if (typeof nmp.setVolume === 'function') {
        nmp.setVolume(window.__quantumAudioState.effectiveMusicVolume);
      }
    } catch (e) {}

    try {
      if (typeof nmp.setEnabled === 'function') nmp.setEnabled(nativeAudioState.music);
    } catch (e) {}

    return nativeAudioState.music && nativeAudioState.master > 0;
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

    syncNativeSettings(null, resolveBaseVolume(audio));

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
      attributeFilter: ['src', 'loop', 'autoplay', 'data-base-volume']
    });
  }
})();
