# plex-bot
Telegram bot for Plex


## commands:
* /searchmovie Movie Title
* /searchtv TV show name

### warnings:
* requesting a tv show currently requests all seasons

### for more features open a ticket

### todo:

* search my imdb/tvdb/tmdb id 
* requesting specific episodes/seasons
* add options to click next and get next result


How to run
--------------


* make an env file with the following (bot.env):
``` 	
OMBI_HOST=<http://www.ombiserver.com:9090>
OMBI_KEY=<ombi api key>
OMBI_BOT_TOKEN=<telegram token>
OMBI_BOT_NAME=<telegram bot name>
```
* the run (bot.env is ex env filename)

`docker run --env-file bot.env -d  stacktraceyo/plexbot`
	
