/** Fecha local en formato yyyy-mm-dd (la que espera <input type="date">), sumando los días indicados. */
export function fechaLocal(dias = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
