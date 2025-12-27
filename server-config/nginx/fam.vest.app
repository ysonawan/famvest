server {
    listen 80;
    server_name famvest.online www.famvest.online;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name famvest.online www.famvest.online;

    ssl_certificate /etc/letsencrypt/live/famvest.online/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/famvest.online/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers (optional but recommended)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options nosniff;
    add_header X-Frame-Options DENY;

    location / {
        proxy_pass http://localhost:8090;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }

    location /ws/ {
        proxy_pass http://localhost:8090;

        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;

        # Optional: increase timeouts to avoid disconnection
        proxy_read_timeout 86400;
        proxy_send_timeout 86400;
    }

    error_page 502 /502.html;
        location = /502.html {
        root /var/www/html;
        internal;
    }
}

