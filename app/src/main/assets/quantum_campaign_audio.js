(function () {
  'use strict';

  var PREFIX = '/assets/sounds/campaign/';
  var nmp = window.QuantumNMP;
  if (!nmp || typeof nmp.play !== 'function') return;

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

  scan();

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
