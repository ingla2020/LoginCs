openssl pkcs12 -export -in file.crt -inkey privcamIVG.key -out test.p12 -name alias -passin pass:keypassphrase -passout pass:certificatepassword
