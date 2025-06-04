FROM postgres:16.0

LABEL maintainer="oress"

COPY init.sh init.sh

RUN chmod +x init.sh

ENTRYPOINT ["/init.sh"]
