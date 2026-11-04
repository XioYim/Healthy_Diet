(() => {
  const menuButton = document.querySelector("[data-menu]");
  const nav = document.querySelector("[data-nav]");
  const toc = document.querySelector("[data-toc]");

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
