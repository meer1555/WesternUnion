/* Smoothly animates a numeric value inside an element — used for the
   balance figure and hero stat counters, for that "premium fintech" feel. */
function animateNumber(el, toValue, opts = {}) {
  const duration = opts.duration || 900;
  const prefix = opts.prefix || '';
  const decimals = opts.decimals != null ? opts.decimals : 2;
  const from = opts.from != null ? opts.from : 0;
  const start = performance.now();

  function frame(now) {
    const progress = Math.min(1, (now - start) / duration);
    const eased = 1 - Math.pow(1 - progress, 3); // ease-out-cubic
    const current = from + (toValue - from) * eased;
    el.textContent = prefix + current.toLocaleString('en-US', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    });
    if (progress < 1) requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);
}
