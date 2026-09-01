/* Lightweight animated particle / constellation background.
   Pure canvas, no dependencies — keeps the "premium" hero feel
   without needing any external animation library. */
(function () {
  const canvas = document.getElementById('particle-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  let width, height, particles;
  const PARTICLE_COUNT = window.innerWidth < 700 ? 45 : 90;
  const MAX_DIST = 140;

  function resize() {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
  }

  function createParticles() {
    particles = Array.from({ length: PARTICLE_COUNT }, () => ({
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.35,
      vy: (Math.random() - 0.5) * 0.35,
      r: Math.random() * 1.8 + 0.6
    }));
  }

  function step() {
    ctx.clearRect(0, 0, width, height);

    for (const p of particles) {
      p.x += p.vx;
      p.y += p.vy;
      if (p.x < 0 || p.x > width) p.vx *= -1;
      if (p.y < 0 || p.y > height) p.vy *= -1;

      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(242, 183, 5, 0.55)';
      ctx.fill();
    }

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const a = particles[i], b = particles[j];
        const dx = a.x - b.x, dy = a.y - b.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < MAX_DIST) {
          ctx.beginPath();
          ctx.moveTo(a.x, a.y);
          ctx.lineTo(b.x, b.y);
          ctx.strokeStyle = `rgba(0, 212, 255, ${0.12 * (1 - dist / MAX_DIST)})`;
          ctx.lineWidth = 1;
          ctx.stroke();
        }
      }
    }

    requestAnimationFrame(step);
  }

  resize();
  createParticles();
  step();

  window.addEventListener('resize', () => {
    resize();
    createParticles();
  });
})();
