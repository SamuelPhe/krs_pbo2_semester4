FROM php:8.3-apache

RUN docker-php-ext-install pdo_mysql \
    && a2enmod headers \
    && printf '%s\\n' \
       '<Directory /var/www/html>' \
       '    Options -Indexes +FollowSymLinks' \
       '    AllowOverride All' \
       '    Require all granted' \
       '</Directory>' \
       > /etc/apache2/conf-available/krs-webapp.conf \
    && a2enconf krs-webapp

WORKDIR /var/www/html
EXPOSE 80
