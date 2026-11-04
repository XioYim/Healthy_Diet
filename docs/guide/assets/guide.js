(() => {
  const menuButton = document.querySelector("[data-menu]");
  const nav = document.querySelector("[data-nav]");
  const toc = document.querySelector("[data-toc]");

  if (nav) {
    const chapters = [
      ["index.html", "总览"], ["nutrition.html", "营养值"], ["fatigue.html", "食物厌倦"],
      ["quality.html", "品质与品级"], ["shelf-life.html", "保质期"], ["tooltip.html", "Tooltip"],
      ["appearance.html", "显示美化"], ["titlepro.html", "TitlePro"],
      ["misc.html", "小功能"], ["admin.html", "服主手册"]
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
      factor.textContent = n <= 5 ? "100%" : n >= 10 ? "0%（厌食）" : `${Math.round((10 - n) / 5 * 100)}%`;
    };
    slider.addEventListener("input", update);
    update();
  }

  const tooltipDemo = document.querySelector("[data-tooltip-demo]");
  if (tooltipDemo) {
    const select = tooltipDemo.querySelector("select");
    const description = tooltipDemo.querySelector("[data-description]");
    const content = {
      normal: "普通提示：物品名和常显行；其他内容由物品布局及显示开关决定。",
      shelf: "S 模式：查看已配置的营养、厌倦和保质期等详情。",
      info: "D 模式：查看说明信息、JEI 内容、附加效果和自定义 infoOnly 行。"
    };
    const update = () => { description.textContent = content[select.value]; };
    select.addEventListener("change", update);
    update();
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
