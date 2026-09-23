export const environment = {
  production: true,
  // En producción, el frontend se sirve detrás del API Gateway (Nginx),
  // que enruta /auth hacia ms-auth-users. Ajustar segun la configuracion
  // final del gateway en vm-gateway (192.168.56.10).
  apiUrl: '/api'
};
