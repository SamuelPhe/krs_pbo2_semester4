FROM php:8.3-apache

RUN docker-php-ext-install pdo_mysql \
    && a2enmod headers \
    && sed -ri '/<Directory \/var\/www\/>/,/<\/Directory>/ s/AllowOverride None/AllowOverride All/' /etc/apache2/apache2.conf

WORKDIR /var/www/html
EXPOSE 80
