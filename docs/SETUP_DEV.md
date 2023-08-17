# Setup sphinx-kotlin on android studio with sphinx-stack local developer environment

- Clone [Sphinx-Stack](https://github.com/stakwork/sphinx-stack)
- Download [Docker Desktop](https://www.docker.com/products/docker-desktop/) available for mac/linux/windows
- Open a terminal inside the root directory of sphinx-stack cloned repository
    - pull all sphinx composables from docker hub `docker-compose pull`
    - run the pulled composables `docker-compose up -d`
- Download and Sign in to get your token from [ngrok](https://ngrok.com/download), opens ngrok app terminal:
    - authorize the token `ngrok config add-authtoken <token>`
    - Start a tunnel `ngrok http <port>`

### for windows users if you had any problems with running the `sphinx-tribe` and `sphinx-db` composables add this lines to the end of your `docker-compose.yml` file :
- `volumes:
  db_volume:
  external: true`

#
open the root directory of sphinx-stack cloned repository in `relay` you'll find a file called `NODES.json` which contains all the fake users information's (alice/bob/carol) you only need :
- port for user ex.`3001`
- `exportedkeys`
- `pin`


### Important: you should run sphinx app on android simulator not a physical device
- run sphinx app and Enter `exportedkeys` & `pin code`
- open user's profile in advanced settings replace `server url` with `ngrok forwading link` and accept changes

Now you should be all setup to develop.

## if you like videos [Sphinx Setup tutorial](https://drive.google.com/file/d/1W81jD0hYx39smUdGp1QUiZNosY2H0aEp/view?usp=sharing)
