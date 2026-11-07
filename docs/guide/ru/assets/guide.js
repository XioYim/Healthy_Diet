(() => {
  const menuButton = document.querySelector("[data-menu]");
  const nav = document.querySelector("[data-nav]");
  const toc = document.querySelector("[data-toc]");

  if (nav) {
    const chapters = [
      ["index.html", "Обзор"], ["introduction.html", "Введение и установка"], ["quick-start.html", "Начало игры"],
      ["nutrition.html", "Питательные вещества"], ["fatigue.html", "Пресыщение едой"],
      ["quality.html", "Качество и редкость"], ["shelf-life.html", "Срок годности"], ["tooltip.html", "Tooltip"],
      ["appearance.html", "Оформление"], ["titlepro.html", "TitlePro"],
      ["misc.html", "Малые функции"], ["admin.html", "Руководство администратора"]
    ];
    const current = location.pathname.split("/").pop() || "index.html";
    for (const [file, title] of chapters) {
      let link = [...nav.querySelectorAll("a")].find(a => a.getAttribute("href") === file);
      if (!link) {
        link = document.createElement("a");
        link.href = file;
        link.textContent = title;
      }
      nav.append(link);
      if (file === current) link.setAttribute("aria-current", "page");
      else link.removeAttribute("aria-current");
    }
  }

  const fatigueDemo = document.querySelector("[data-fatigue-demo]");
  if (fatigueDemo) {
    const slider = fatigueDemo.querySelector("input");
    const count = fatigueDemo.querySelector("[data-count]");
    const factor = fatigueDemo.querySelector("[data-factor]");
    const update = () => {
      const n = Number(slider.value);
      count.textContent = String(n);
      factor.textContent = n <= 5 ? "100%" : n >= 10 ? "0% (отвращение)" : `${Math.round((10 - n) / 5 * 100)}%`;
    };
    slider.addEventListener("input", update);
    update();
  }

  const tooltipDemo = document.querySelector("[data-tooltip-demo]");
  if (tooltipDemo) {
    const select = tooltipDemo.querySelector("select");
    const description = tooltipDemo.querySelector("[data-description]");
    const content = {
      normal: "Обычное наведение: название предмета и постоянные строки; остальное зависит от макета и переключателей.",
      shelf: "Режим S: подробности настроенного питания, пресыщения и срока годности.",
      info: "Режим D: описание, содержимое JEI, дополнительные эффекты и пользовательские строки infoOnly."
    };
    const update = () => { description.textContent = content[select.value]; };
    select.addEventListener("change", update);
    update();
  }

  const shelfTabs = [...document.querySelectorAll("[data-shelf-tabs] [role='tab']")];
  if (shelfTabs.length) {
    const selectShelfTab = tab => {
      shelfTabs.forEach(button => {
        const active = button === tab;
        button.setAttribute("aria-selected", String(active));
        button.tabIndex = active ? 0 : -1;
        document.getElementById(button.getAttribute("aria-controls")).hidden = !active;
      });
    };
    shelfTabs.forEach((tab, index) => {
      tab.addEventListener("click", () => selectShelfTab(tab));
      tab.addEventListener("keydown", event => {
        if (!["ArrowRight", "ArrowLeft", "Home", "End"].includes(event.key)) return;
        event.preventDefault();
        const next = event.key === "Home" ? 0 : event.key === "End" ? shelfTabs.length - 1
          : (index + (event.key === "ArrowRight" ? 1 : -1) + shelfTabs.length) % shelfTabs.length;
        shelfTabs[next].focus();
        selectShelfTab(shelfTabs[next]);
      });
    });
  }

  const sampleData = document.getElementById("shelf-sample-data");
  if (sampleData) {
    const samples = JSON.parse(sampleData.textContent).cases;
    const buttons = [...document.querySelectorAll("[data-case-id]")];
    const search = document.querySelector("[data-shelf-search]");
    const searchStatus = document.querySelector("[data-shelf-search-status]");
    const viewer = document.querySelector("[data-shelf-viewer]");
    const importStatus = document.querySelector("[data-shelf-import-status]");
    const number = value => typeof value === "number" && Number.isFinite(value) ? String(value) : "未提供";
    const temp = value => number(value) === "未提供" ? "未提供" : `${number(value)}℃`;
    const row = (parent, label, value) => {
      const entry = document.createElement("div");
      entry.className = "shelf-info-row";
      const key = document.createElement("b");
      key.textContent = label;
      const text = document.createElement("span");
      text.textContent = value;
      entry.append(key, text);
      parent.append(entry);
    };
    const stat = (parent, label, value) => {
      const entry = document.createElement("div");
      entry.className = "shelf-stat";
      const key = document.createElement("small");
      key.textContent = label;
      const text = document.createElement("b");
      text.textContent = value;
      entry.append(key, text);
      parent.append(entry);
    };
    // 默认数据包中仍有旧四槽；按查询器的迁移规则转成现行效果列表，不展示空槽。
    const effectsFor = shelf => {
      const current = Array.isArray(shelf.temperatureEffectCurves) ? shelf.temperatureEffectCurves.slice() : [];
      const legacy = shelf.temperatureZoneEffects;
      if (!Array.isArray(legacy) || legacy.length !== 4 || !shelf.common) return current;
      const c = shelf.common;
      const bounds = [c.fullyFrozenC, c.freezingStartC, c.coldFreshStartC, c.highTemperatureC, c.eatableMaxTemperatureC];
      if (!bounds.every(Number.isFinite)) return current;
      const migrated = [];
      legacy.forEach((effect, index) => {
        if (!effect?.effectId || bounds[index] > bounds[index + 1]
            || (bounds[index] === bounds[index + 1] && index !== 3)) return;
        const cold = ["legendarysurvivaloverhaul:cold_food", "legendarysurvivaloverhaul:cold_drink"].includes(effect.effectId);
        const single = bounds[index] === bounds[index + 1];
        migrated.push({
          effectId: effect.effectId, startTemperatureC: bounds[index], endTemperatureC: bounds[index + 1],
          includeStart: index !== 0, includeEnd: index === 3,
          startLevel: single ? effect.maxLevel : cold ? effect.maxLevel : effect.minLevel,
          endLevel: single ? effect.maxLevel : cold ? effect.minLevel : effect.maxLevel,
          startDuration: single ? effect.maxDuration : cold ? effect.maxDuration : effect.minDuration,
          endDuration: single ? effect.maxDuration : cold ? effect.minDuration : effect.maxDuration,
          particles: effect.particles
        });
      });
      return migrated.concat(current);
    };
    const show = (query, builtIn = false) => {
      const shelf = query.config.shelf;
      if (!shelf || typeof shelf !== "object" || Array.isArray(shelf)) {
        viewer.hidden = true;
        importStatus.textContent = "此物品没有保质期配置；没有可展示的 shelf 数据。";
        return;
      }
      viewer.hidden = false;
      viewer.querySelector("[data-shelf-origin]").textContent = builtIn ? "内置数据包 · 只读示例" : "本地 /hdinfo 查询 · 只读";
      viewer.querySelector("[data-shelf-name]").textContent = query.itemName || query.itemId;
      viewer.querySelector("[data-shelf-id]").textContent = query.itemId;
      const stats = viewer.querySelector("[data-shelf-stats]");
      const boundaries = viewer.querySelector("[data-shelf-boundaries]");
      const rules = viewer.querySelector("[data-shelf-rules]");
      const effects = viewer.querySelector("[data-shelf-effects]");
      const environment = viewer.querySelector("[data-shelf-environment]");
      [stats, boundaries, rules, effects, environment].forEach(container => container.replaceChildren());
      const minutes = shelf.durationMinutes;
      const units = {minutes: [1, "分钟"], hours: [60, "小时"], days: [1440, "天"], game_days: [20, "游戏日"]};
      const [factor, unit] = units[shelf.durationUnit] || units.minutes;
      stat(stats, "基础时长", typeof minutes === "number" ? `${number(minutes)} 分钟` : "未提供");
      stat(stats, "编辑器单位", typeof minutes === "number" ? `${number(minutes / factor)} ${unit}` : "未提供");
      stat(stats, "基础变质倍率", shelf.baseDecayMultiplier === undefined ? "未提供" : `×${number(shelf.baseDecayMultiplier)}`);
      stat(stats, "初始温度", temp(shelf.common?.initialTemperatureC));
      row(boundaries, "物品温度范围", `${temp(shelf.common?.minTemperatureC)} ～ ${temp(shelf.common?.maxTemperatureC)}`);
      row(boundaries, "可食温度范围", `${temp(shelf.common?.eatableMinTemperatureC)} ～ ${temp(shelf.common?.eatableMaxTemperatureC)}`);
      row(boundaries, "完全冻结 / 冷冻 / 冷鲜", `${temp(shelf.common?.fullyFrozenC)} / ${temp(shelf.common?.freezingStartC)} / ${temp(shelf.common?.coldFreshStartC)}`);
      row(boundaries, "高温点 / 50% 冷鲜保鲜率", `${temp(shelf.common?.highTemperatureC)} / ${shelf.common?.preservationRate === undefined ? "未提供" : `${number(shelf.common.preservationRate * 100)}%`}`);
      const result = shelf.resultItem === "minecraft:air" ? "消失 (minecraft:air)" : shelf.resultItem || "未提供";
      row(boundaries, "过期后 / 继承产物温度", `${result} / ${shelf.inheritTemperatureOnExpire ? "开启" : "关闭或未提供"}`);
      row(boundaries, "过期指令", shelf.expireCommand || "未配置");
      const curves = Array.isArray(shelf.temperatureCurves) ? shelf.temperatureCurves : [];
      if (!curves.length) row(rules, "温度变质曲线", "未配置 (不额外改变曲线倍率)");
      curves.forEach((curve, index) => row(rules, `温度曲线 ${index + 1}`,
        `${temp(curve.startTemperatureC)} ～ ${temp(curve.endTemperatureC)}：×${number(curve.startMultiplier)} → ×${number(curve.endMultiplier)}`
        + (curve.startBufferTemperatureC != null ? `；起缓冲 ${temp(curve.startBufferTemperatureC)}` : "")
        + (curve.endBufferTemperatureC != null ? `；止缓冲 ${temp(curve.endBufferTemperatureC)}` : "")));
      const dimensions = shelf.dimensionDecayMultipliers && typeof shelf.dimensionDecayMultipliers === "object"
        ? Object.entries(shelf.dimensionDecayMultipliers) : [];
      if (!dimensions.length) row(rules, "维度变质倍率", "未配置 (其余维度 ×1)");
      dimensions.forEach(([id, multiplier]) => row(rules, `维度 ${id}`, `变质倍率 ×${number(multiplier)}，不是温度偏移`));
      row(rules, "保冷类型", shelf.coldRetention
        ? `开启；完全冻结回充 ${number(shelf.coldRecoveryRate)} 分钟/分钟；耗损档位 ${number(shelf.coldDegradationRate)}%`
        : "未开启 (不会回充保冷时间)");
      const curvesForEating = effectsFor(shelf);
      if (!curvesForEating.length) row(effects, "温度附加效果", "未配置；不自动生成四个固定温区效果");
      curvesForEating.forEach((effect, index) => row(effects, `自定义区 ${index + 1} · ${effect.effectId || "未提供效果 ID"}`,
        `${effect.includeStart === false ? "(" : "["}${number(effect.startTemperatureC)}～${number(effect.endTemperatureC)}${effect.includeEnd === false ? ")" : "]"}℃；` +
        `等级 ${number(typeof effect.startLevel === "number" ? effect.startLevel + 1 : undefined)}～${number(typeof effect.endLevel === "number" ? effect.endLevel + 1 : undefined)} 级；` +
        `${number(typeof effect.startDuration === "number" ? effect.startDuration / 20 : undefined)}～${number(typeof effect.endDuration === "number" ? effect.endDuration / 20 : undefined)} 秒；` +
        `${effect.particles === false ? "隐藏" : "显示"}粒子`));
      const natural = shelf.naturalEnvironment;
      const fridge = shelf.fridgeTemperature;
      row(environment, "自然环境基础速度", natural ? `${temp(natural.temperatureChangeC)} / ${number(natural.temperatureChangeMinutes)} 分钟` : "此查询未包含自然环境字段");
      const bonusKeys = ["freezingSpeedMultiplierBonus", "lowTemperatureSpeedMultiplierBonus", "normalTemperatureSpeedMultiplierBonus", "highTemperatureSpeedMultiplierBonus"];
      const bonusNames = ["冷冻", "低温", "常温", "高温"];
      if (natural) bonusKeys.forEach((key, index) => {
        if (natural[key]) row(environment, `自然环境 ${bonusNames[index]}温变修正`, `+${number(natural[key])} 倍`);
      });
      row(environment, "普通冰箱基础速度", fridge ? `${temp(fridge.fridgeTemperatureChangeC)} / ${number(fridge.fridgeTemperatureChangeMinutes)} 分钟` : "此查询未包含冰箱字段");
      row(environment, "冷冻室基础速度", fridge ? `${temp(fridge.freezerTemperatureChangeC)} / ${number(fridge.freezerTemperatureChangeMinutes)} 分钟` : "此查询未包含冷冻室字段");
      if (fridge) bonusKeys.forEach((key, index) => {
        if (fridge[key]) row(environment, `冰箱 ${bonusNames[index]}温变修正`, `+${number(fridge[key])} 倍`);
      });
      const path = query.itemId.replace(":", "/healthy_diet/items/") + ".json";
      viewer.querySelector("[data-shelf-source]").textContent = builtIn
        ? `来源：src/main/resources/defaults/datapack/data/${path}；本页仅摘录保质期字段，旧四区按当前查询器规则迁移。`
        : "来源：你在本地粘贴的 /hdinfo 查询 JSON；仅展示其中的配置，不包含物品运行中的剩余时长或当前温度。";
    };
    const selectSample = button => {
      buttons.forEach(item => item.setAttribute("aria-pressed", String(item === button)));
      show(samples.find(sample => sample.itemId === button.dataset.caseId), true);
    };
    buttons.forEach(button => button.addEventListener("click", () => selectSample(button)));
    search.addEventListener("input", () => {
      const term = search.value.trim().toLocaleLowerCase();
      buttons.forEach(button => {
        button.hidden = !`${button.dataset.caseId} ${button.textContent}`.toLocaleLowerCase().includes(term);
      });
      const visible = buttons.filter(button => !button.hidden);
      searchStatus.textContent = visible.length ? `匹配 ${visible.length} 个内置案例；未连接服务器。`
        : "没有匹配的内置案例；可以在下方粘贴自己的 /hdinfo 查询 JSON。";
      if (!visible.length) viewer.hidden = true;
      else if (!visible.some(button => button.getAttribute("aria-pressed") === "true")) selectSample(visible[0]);
      else viewer.hidden = false;
    });
    document.querySelector("[data-shelf-import]").addEventListener("click", () => {
      const text = document.querySelector("[data-shelf-json]").value.trim();
      if (!text || text.length > 4000000) {
        importStatus.textContent = "请粘贴长度不超过 400 万字符的单物品查询 JSON。";
        return;
      }
      try {
        const query = JSON.parse(text);
        if (!query || query.schemaVersion !== 1 || typeof query.itemId !== "string" || !query.itemId.includes(":")
            || !query.config || typeof query.config !== "object" || Array.isArray(query.config)) {
          throw new Error("需要 schemaVersion: 1、itemId 和 config 对象");
        }
        buttons.forEach(button => button.setAttribute("aria-pressed", "false"));
        show(query);
        if (query.config.shelf) importStatus.textContent = `已在本地读取 ${query.itemId} 的保质期配置；没有上传或保存。`;
      } catch (error) {
        importStatus.textContent = `无法读取 JSON：${error.message}`;
      }
    });
    selectSample(buttons[0]);
  }

  if (menuButton && nav) {
    menuButton.addEventListener("click", () => {
      const open = nav.classList.toggle("open");
      menuButton.setAttribute("aria-expanded", String(open));
    });
  }

  if (toc) {
    const links = [...toc.querySelectorAll('a[href^="#"]')];
    const sections = links
      .map(link => document.querySelector(link.getAttribute("href")))
      .filter(Boolean);

    if (sections.length && "IntersectionObserver" in window) {
      const observer = new IntersectionObserver(entries => {
        const visible = entries
          .filter(entry => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (!visible.length) return;
        const id = visible[0].target.id;
        links.forEach(link => {
          link.classList.toggle("active", link.getAttribute("href") === "#" + id);
        });
      }, { rootMargin: "-80px 0px -70% 0px", threshold: 0 });
      sections.forEach(section => observer.observe(section));
    }
  }
})();
