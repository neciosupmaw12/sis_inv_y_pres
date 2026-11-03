export const environment = {
  production: true,
  // La API está en el mismo equipo que sirve el frontend: funciona con localhost y con la IP de la red
  apiUrl: `${window.location.protocol}//${window.location.hostname}:8080/api`,
};
