# Gateway (vm-gateway, 192.168.56.10)

Configuración de Nginx como reverse proxy / API Gateway y servidor de
archivos estáticos del frontend Angular.

## Contenido

- `nginx/neobanco.conf`: configuración del `server` de Nginx (sirve
  `frontend/dist/frontend/browser` y hace proxy de `/api/` hacia
  `ms-auth-users` en `vm-backend:8080`).

## Instalación manual (primera vez en vm-gateway)

```bash
sudo apt update && sudo apt install -y nginx
sudo mkdir -p /var/www/neobanco/frontend
sudo cp gateway/nginx/neobanco.conf /etc/nginx/sites-available/neobanco.conf
sudo ln -s /etc/nginx/sites-available/neobanco.conf /etc/nginx/sites-enabled/neobanco.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

## Despliegue del frontend

El job `deploy-to-gateway` de `.github/workflows/deploy-frontend.yml`
copia `frontend/dist/frontend/browser` a `/var/www/neobanco/frontend`
en cada push a `main` que toque `frontend/**`, usando el secreto SSH
`PRIVATE_KEY` configurado en el repositorio.

## Pendiente

- Agregar más microservicios al proxy (`/api/cuentas`, etc.) a medida
  que se implementen.
- HTTPS (certificado + redirección 80 -> 443).
