(() => {
  const menuButton = document.querySelector('[data-menu]');
  const nav = document.querySelector('[data-nav]');
  if (menuButton && nav) menuButton.addEventListener('click', () => {
    const expanded = nav.classList.toggle('open');
    menuButton.setAttribute('aria-expanded', String(expanded));
  });

  const toc = document.querySelector('[data-toc]');
  if (toc && 'IntersectionObserver' in window) {
    const links = [...toc.querySelectorAll('a[href^="#"]')];
    const sections = links.map(a => document.getElementById(a.hash.slice(1))).filter(Boolean);
    const observer = new IntersectionObserver(entries => {
      const visible = entries.filter(entry => entry.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
      if (visible.length) links.forEach(link => link.classList.toggle('active', link.hash === '#' + visible[0].target.id));
    }, {rootMargin: '-80px 0px -70% 0px'});
    sections.forEach(section => observer.observe(section));
  }

  const fatigue = document.querySelector('[data-fatigue-demo]');
  if (fatigue) {
    const slider = fatigue.querySelector('input');
    const update = () => {
      const count = Number(slider.value);
      fatigue.querySelector('[data-count]').textContent = String(count);
      fatigue.querySelector('[data-factor]').textContent = count <= 5 ? '100%' : count >= 10 ? '0% (aversion)' : Math.round((10 - count) / 5 * 100) + '%';
    };
    slider.addEventListener('input', update);
    update();
  }

  const tooltip = document.querySelector('[data-tooltip-demo]');
  if (tooltip) {
    const select = tooltip.querySelector('select');
    const descriptions = {
      normal: 'Normal hover: item name and always-visible lines; layout and visibility settings determine the rest.',
      shelf: 'S mode: details about configured nutrition, fatigue and shelf life.',
      info: 'D mode: descriptions, JEI material, extra effects and custom infoOnly lines.'
    };
    const update = () => { tooltip.querySelector('[data-description]').textContent = descriptions[select.value]; };
    select.addEventListener('change', update);
    update();
  }

  for (const bar of document.querySelectorAll('[data-shelf-tabs]')) {
    const tabs = [...bar.querySelectorAll('[role="tab"]')];
    const select = tab => tabs.forEach(item => {
      const selected = item === tab;
      item.setAttribute('aria-selected', String(selected));
      item.tabIndex = selected ? 0 : -1;
      document.getElementById(item.getAttribute('aria-controls')).hidden = !selected;
    });
    tabs.forEach((tab, index) => {
      tab.addEventListener('click', () => select(tab));
      tab.addEventListener('keydown', event => {
        if (!['ArrowRight', 'ArrowLeft', 'Home', 'End'].includes(event.key)) return;
        event.preventDefault();
        const next = event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : (index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length;
        select(tabs[next]);
        tabs[next].focus();
      });
    });
  }

  const filter = document.querySelector('[data-config-filter]');
  if (filter) {
    const rows = [...document.querySelectorAll('[data-config-table] tbody tr')];
    const status = document.querySelector('[data-config-status]');
    const update = () => {
      const query = filter.value.trim().toLocaleLowerCase('en');
      let shown = 0;
      rows.forEach(row => {
        row.hidden = !row.textContent.toLocaleLowerCase('en').includes(query);
        if (!row.hidden) shown++;
      });
      status.textContent = query ? `Matching entries: ${shown} / ${rows.length}` : `${rows.length} configuration locations; filter by path, scope or field.`;
    };
    filter.addEventListener('input', update);
    update();
  }

  const stages = document.getElementById('stage-range');
  if (stages && !document.body.dataset.nutritionBonus) {
    const update = () => {
      const value = Number(stages.value);
      const rounded = Math.round(value);
      document.getElementById('stage-marker').style.left = value + '%';
      document.getElementById('stage-value').textContent = value.toFixed(1);
      document.getElementById('stage-rounded').textContent = String(rounded);
      document.getElementById('stage-name').textContent = rounded < 30 ? 'Low' : rounded < 70 ? 'Mid' : 'High';
    };
    stages.addEventListener('input', update);
    update();
  }
  const decay = document.getElementById('decay-rate');
  if (decay && !document.body.dataset.nutritionBonus) {
    const hunger = document.getElementById('hunger-used');
    const update = () => {
      document.getElementById('decay-rate-out').textContent = decay.value;
      document.getElementById('hunger-used-out').textContent = hunger.value;
      const loss = Number(hunger.value) / Number(decay.value);
      document.getElementById('decay-loss').textContent = loss.toFixed(1);
      document.getElementById('decay-remain').textContent = Math.max(0, 100 - loss).toFixed(1);
    };
    decay.addEventListener('input', update);
    hunger.addEventListener('input', update);
    update();
  }
  const rows = document.getElementById('mock-rows');
  if (rows && !document.body.dataset.nutritionBonus) {
    const groups = [
      {name: 'Vegetables', value: 55, colors: ['#FDFC47', '#3DF750'], stages: [[0, 10], [11, 45], [46, 60], [61, 70], [71, 80], [81, 100]]},
      {name: 'Fruits', value: 55, colors: ['#f4c4f3', '#fc67fa'], stages: [[0, 10], [11, 45], [46, 60], [61, 70], [71, 80], [81, 100]]},
      {name: 'Protein', value: 65, colors: ['#ff6a6e', '#ffc371'], stages: [[0, 10], [11, 45], [46, 60], [61, 70], [71, 80], [81, 100]]},
      {name: 'Grains', value: 65, colors: ['#78350F', '#FDE68A'], stages: [[0, 10], [11, 45], [46, 60], [61, 70], [71, 80], [81, 100]]},
      {name: 'Sugar', value: 88, colors: ['#b3ffab', '#12fff7'], stages: [[0, 10], [11, 50], [51, 85], [86, 92], [93, 100]]},
      {name: 'Dairy', value: 88, colors: ['#8e9eab', '#eef2f3'], stages: [[0, 10], [11, 50], [51, 85], [86, 92], [93, 100]]}
    ];
    for (const group of groups) {
      const row = document.createElement('div');
      row.className = 'mock-row';
      row.style.setProperty('--c', group.colors[0]);
      row.style.setProperty('--text-color', group.colors.at(-1));
      row.style.setProperty('--gradient', `linear-gradient(90deg, ${group.colors.join(',')})`);
      const icon = document.createElement('span'); icon.className = 'mock-icon'; icon.setAttribute('aria-hidden', 'true');
      const name = document.createElement('span'); name.className = 'mock-name'; name.textContent = group.name;
      const track = document.createElement('div'); track.className = 'mock-track';
      const fill = document.createElement('span'); fill.className = 'mock-fill'; fill.style.width = group.value + '%'; track.append(fill);
      const tip = document.createElement('div'); tip.className = 'mock-tip'; tip.setAttribute('role', 'tooltip');
      for (let i = 0; i < group.stages.length; i++) {
        const [lo, hi] = group.stages[i];
        const segment = document.createElement('button');
        segment.className = 'mock-seg'; segment.type = 'button';
        segment.style.left = lo + '%'; segment.style.width = ((group.stages[i + 1]?.[0] ?? 100) - lo) + '%';
        const details = `${group.name}: ${lo}–${hi} · stage range from bundled Forge configuration (bonuses depend on other groups)`;
        segment.setAttribute('aria-label', details);
        segment.addEventListener('mouseenter', () => { tip.textContent = details; });
        segment.addEventListener('focus', () => { tip.textContent = details; });
        track.append(segment);
      }
      track.append(tip);
      const value = document.createElement('span'); value.className = 'mock-pct'; value.textContent = group.value.toFixed(1) + '%';
      row.append(icon, name, track, value); rows.append(row);
    }
  }

  const sampleData = document.getElementById('shelf-sample-data');
  if (sampleData) {
    const samples = JSON.parse(sampleData.textContent).cases;
    const buttons = [...document.querySelectorAll('[data-case-id]')];
    const viewer = document.querySelector('[data-shelf-viewer]');
    const status = document.querySelector('[data-shelf-import-status]');
    const search = document.querySelector('[data-shelf-search]');
    const searchStatus = document.querySelector('[data-shelf-search-status]');
    const fmt = value => typeof value === 'number' && Number.isFinite(value) ? String(value) : 'Not provided';
    const degrees = value => typeof value === 'number' && Number.isFinite(value) ? `${value}°C` : 'Not provided';
    const entry = (container, label, value, stat = false) => {
      const row = document.createElement('div'); row.className = stat ? 'shelf-stat' : 'shelf-info-row';
      const key = document.createElement(stat ? 'small' : 'b'); key.textContent = label;
      const text = document.createElement(stat ? 'b' : 'span'); text.textContent = value;
      row.append(key, text); container.append(row);
    };
    const legacyEffects = shelf => {
      const current = Array.isArray(shelf.temperatureEffectCurves) ? shelf.temperatureEffectCurves.slice() : [];
      const migrated = [];
      const old = shelf.temperatureZoneEffects, c = shelf.common;
      if (!Array.isArray(old) || old.length !== 4 || !c) return current;
      const bounds = [c.fullyFrozenC, c.freezingStartC, c.coldFreshStartC, c.highTemperatureC, c.eatableMaxTemperatureC];
      if (!bounds.every(Number.isFinite)) return current;
      old.forEach((effect, index) => {
        if (!effect?.effectId || bounds[index] > bounds[index + 1] || (bounds[index] === bounds[index + 1] && index !== 3)) return;
        const cold = ['legendarysurvivaloverhaul:cold_food', 'legendarysurvivaloverhaul:cold_drink'].includes(effect.effectId);
        const single = bounds[index] === bounds[index + 1];
        migrated.push({effectId: effect.effectId, startTemperatureC: bounds[index], endTemperatureC: bounds[index + 1],
          includeStart: index !== 0, includeEnd: index === 3,
          startLevel: single || cold ? effect.maxLevel : effect.minLevel,
          endLevel: single || !cold ? effect.maxLevel : effect.minLevel,
          startDuration: single || cold ? effect.maxDuration : effect.minDuration,
          endDuration: single || !cold ? effect.maxDuration : effect.minDuration, particles: effect.particles});
      });
      return migrated.concat(current);
    };
    const show = (query, builtIn = false) => {
      const shelf = query.config?.shelf;
      if (!shelf || typeof shelf !== 'object' || Array.isArray(shelf)) {
        viewer.hidden = true; status.textContent = 'This item has no shelf-life configuration to display.'; return;
      }
      viewer.hidden = false;
      viewer.querySelector('[data-shelf-origin]').textContent = builtIn ? 'Bundled datapack · read-only' : 'Local /hdinfo JSON · read-only';
      viewer.querySelector('[data-shelf-name]').textContent = query.itemName || query.itemId;
      viewer.querySelector('[data-shelf-id]').textContent = query.itemId;
      const stats = viewer.querySelector('[data-shelf-stats]'), boundaries = viewer.querySelector('[data-shelf-boundaries]');
      const rules = viewer.querySelector('[data-shelf-rules]'), effects = viewer.querySelector('[data-shelf-effects]');
      const environment = viewer.querySelector('[data-shelf-environment]');
      [stats, boundaries, rules, effects, environment].forEach(el => el.replaceChildren());
      const unit = {minutes: [1, 'minutes'], hours: [60, 'hours'], days: [1440, 'real days'], game_days: [20, 'game days']}[shelf.durationUnit] || [1, 'minutes'];
      entry(stats, 'Base duration', typeof shelf.durationMinutes === 'number' ? `${fmt(shelf.durationMinutes)} minutes` : 'Not provided', true);
      entry(stats, 'Editor units', typeof shelf.durationMinutes === 'number' ? `${fmt(shelf.durationMinutes / unit[0])} ${unit[1]}` : 'Not provided', true);
      entry(stats, 'Base decay multiplier', shelf.baseDecayMultiplier == null ? 'Not provided' : `×${fmt(shelf.baseDecayMultiplier)}`, true);
      entry(stats, 'Initial temperature', degrees(shelf.common?.initialTemperatureC), true);
      entry(boundaries, 'Item temperature bounds', `${degrees(shelf.common?.minTemperatureC)} to ${degrees(shelf.common?.maxTemperatureC)}`);
      entry(boundaries, 'Edible temperature range', `${degrees(shelf.common?.eatableMinTemperatureC)} to ${degrees(shelf.common?.eatableMaxTemperatureC)}`);
      entry(boundaries, 'Fully frozen / freezing / cold-fresh', `${degrees(shelf.common?.fullyFrozenC)} / ${degrees(shelf.common?.freezingStartC)} / ${degrees(shelf.common?.coldFreshStartC)}`);
      entry(boundaries, 'Hot threshold / preservation at 50% fresh', `${degrees(shelf.common?.highTemperatureC)} / ${shelf.common?.preservationRate == null ? 'Not provided' : shelf.common.preservationRate * 100 + '%'}`);
      entry(boundaries, 'On expiration / inherit result temperature', `${shelf.resultItem === 'minecraft:air' ? 'Disappear (minecraft:air)' : shelf.resultItem || 'Not provided'} / ${shelf.inheritTemperatureOnExpire ? 'Yes' : 'No or unspecified'}`);
      entry(boundaries, 'Expiration command', shelf.expireCommand || 'Not configured');
      const curves = Array.isArray(shelf.temperatureCurves) ? shelf.temperatureCurves : [];
      if (!curves.length) entry(rules, 'Temperature decay curves', 'Not configured (no additional curve multiplier)');
      curves.forEach((curve, i) => entry(rules, `Curve ${i + 1}`, `${degrees(curve.startTemperatureC)} to ${degrees(curve.endTemperatureC)}: ×${fmt(curve.startMultiplier)} → ×${fmt(curve.endMultiplier)}` + (curve.startBufferTemperatureC != null ? `; start buffer ${degrees(curve.startBufferTemperatureC)}` : '') + (curve.endBufferTemperatureC != null ? `; end buffer ${degrees(curve.endBufferTemperatureC)}` : '')));
      const dimensions = Object.entries(shelf.dimensionDecayMultipliers || {});
      if (!dimensions.length) entry(rules, 'Dimension multipliers', 'Not configured (other dimensions ×1)');
      dimensions.forEach(([id, multiplier]) => entry(rules, id, `Spoilage ×${fmt(multiplier)}, not a temperature offset`));
      entry(rules, 'Cold retention', shelf.coldRetention ? `Enabled; recovery ${fmt(shelf.coldRecoveryRate)} minutes/minute when fully frozen; permanent loss step ${fmt(shelf.coldDegradationRate)}%` : 'Disabled (no time recovery)');
      const zones = legacyEffects(shelf);
      if (!zones.length) entry(effects, 'Temperature effects', 'None configured; no four fixed zones are generated');
      zones.forEach((effect, i) => entry(effects, `Custom zone ${i + 1} · ${effect.effectId || 'Unknown effect ID'}`, `${effect.includeStart === false ? '(' : '['}${fmt(effect.startTemperatureC)} to ${fmt(effect.endTemperatureC)}${effect.includeEnd === false ? ')' : ']'}°C; levels ${fmt(typeof effect.startLevel === 'number' ? effect.startLevel + 1 : undefined)}–${fmt(typeof effect.endLevel === 'number' ? effect.endLevel + 1 : undefined)}; ${fmt(typeof effect.startDuration === 'number' ? effect.startDuration / 20 : undefined)}–${fmt(typeof effect.endDuration === 'number' ? effect.endDuration / 20 : undefined)} seconds; particles ${effect.particles === false ? 'hidden' : 'shown'}`));
      const n = shelf.naturalEnvironment, f = shelf.fridgeTemperature;
      entry(environment, 'Natural baseline', n ? `${degrees(n.temperatureChangeC)} / ${fmt(n.temperatureChangeMinutes)} minutes` : 'No natural-environment fields in this query');
      const bonusKeys = ['freezingSpeedMultiplierBonus', 'lowTemperatureSpeedMultiplierBonus', 'normalTemperatureSpeedMultiplierBonus', 'highTemperatureSpeedMultiplierBonus'];
      const bonusNames = ['frozen', 'low', 'normal', 'hot'];
      if (n) bonusKeys.forEach((key, i) => { if (n[key]) entry(environment, `Natural ${bonusNames[i]} speed bonus`, `+${fmt(n[key])}×`); });
      entry(environment, 'Fridge baseline', f ? `${degrees(f.fridgeTemperatureChangeC)} / ${fmt(f.fridgeTemperatureChangeMinutes)} minutes` : 'No fridge fields in this query');
      entry(environment, 'Freezer baseline', f ? `${degrees(f.freezerTemperatureChangeC)} / ${fmt(f.freezerTemperatureChangeMinutes)} minutes` : 'No freezer fields in this query');
      if (f) bonusKeys.forEach((key, i) => { if (f[key]) entry(environment, `Fridge ${bonusNames[i]} speed bonus`, `+${fmt(f[key])}×`); });
      viewer.querySelector('[data-shelf-source]').textContent = builtIn ? `Source: src/main/resources/defaults/datapack/data/${query.itemId.replace(':', '/healthy_diet/items/')}.json; only shelf fields are projected and old four-zone effects are migrated.` : 'Source: locally pasted /hdinfo query JSON. Does not contain live remaining time or current item temperature.';
    };
    const selectSample = button => {
      buttons.forEach(item => item.setAttribute('aria-pressed', String(item === button)));
      show(samples.find(sample => sample.itemId === button.dataset.caseId), true);
    };
    buttons.forEach(button => button.addEventListener('click', () => selectSample(button)));
    search.addEventListener('input', () => {
      const term = search.value.trim().toLocaleLowerCase('en');
      buttons.forEach(button => { button.hidden = !`${button.dataset.caseId} ${button.textContent}`.toLocaleLowerCase('en').includes(term); });
      const visible = buttons.filter(button => !button.hidden);
      searchStatus.textContent = visible.length ? `Matches: ${visible.length} bundled examples; not connected to a server.` : 'No matching built-in examples. Paste your own /hdinfo JSON below.';
      if (!visible.length) viewer.hidden = true;
      else if (!visible.some(button => button.getAttribute('aria-pressed') === 'true')) selectSample(visible[0]);
      else viewer.hidden = false;
    });
    document.querySelector('[data-shelf-import]').addEventListener('click', () => {
      const text = document.querySelector('[data-shelf-json]').value.trim();
      if (!text || text.length > 4000000) { status.textContent = 'Paste one item query JSON, at most 4 million characters.'; return; }
      try {
        const query = JSON.parse(text);
        if (query?.schemaVersion !== 1 || typeof query.itemId !== 'string' || !query.itemId.includes(':') || !query.config || typeof query.config !== 'object' || Array.isArray(query.config)) throw Error('Expected schemaVersion: 1, itemId and a config object');
        buttons.forEach(button => button.setAttribute('aria-pressed', 'false'));
        show(query);
        if (query.config.shelf) status.textContent = `Loaded the shelf configuration for ${query.itemId} locally; nothing was uploaded or saved.`;
      } catch (error) { status.textContent = `Could not read JSON: ${error.message}`; }
    });
    selectSample(buttons[0]);
  }
})();
