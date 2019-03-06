
# ombi-bot ![](https://img.shields.io/docker/pulls/stacktraceyo/ombi-bot.svg)
Telegram bot for Plex
- v1 (OMBI Requests) 


## commands:
* /searchmovie Movie Title
* /searchtv TV show name
* /info

### warning :
* requesting a tv show currently requests all seasons

### for more features open a ticket

### todo:

* search by imdb/tvdb/tmdb id 
* requesting specific episodes/seasons
* cleanup and testing


How to run
--------------

##### with docker

* make an env file with the following (bot.env):
``` 	
OMBI_HOST=<http://www.ombiserver.com:9090>
OMBI_KEY=<ombi api key>
OMBI_BOT_TOKEN=<telegram token>
OMBI_BOT_NAME=<telegram bot name>
```
* the run (bot.env is ex env filename)

`docker run --env-file bot.env -d  stacktraceyo/ombi-bot`
	
https://hub.docker.com/r/stacktraceyo/ombi-bot


##### build jar

`mvn clean install -DskipTests`

or download it from :



