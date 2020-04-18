
# ombi-bot ![](https://img.shields.io/docker/pulls/stacktraceyo/ombi-bot.svg)
Telegram bot for Ombi Requests
- v2.0


## commands:
* /searchmovie Movie Title or imdb url
* /searchtv TV show name or imdb url
* /search Search both Movie and TV show name or imdb url
* /info

### warning :
* requesting a tv show currently requests all seasons

### for more features open a ticket

### todo:
 
* requesting specific episodes/seasons
* testing


How to run
--------------


* make an env file with the following (bot.env):
``` 	
OMBI_HOST=<http://www.ombiserver.com:9090> // The url to ombi instance
OMBI_KEY=<ombi api key> // ombi api key
OMBI_BOT_TOKEN=<telegram token> // telegram bot token
OMBI_BOT_NAME=<telegram bot name> // name of telegram bot
OMBI_USER_NAME=<ombi admin name> (OPTIONAL) // ombi admin username 
```
* the run (bot.env is ex env filename)

##### with docker

`docker run --env-file bot.env -d  stacktraceyo/ombi-bot`


##### without docker

* install java version 8+ and maven
* clone project `git clone https://github.com/StackTraceYo/ombi-bot`
* cd into directory and run `mvn clean install`
* get the output jar at `ombi-bot/bots/target/plexbot.jar`
* run the jar directly with the env file (bot.env is ex env filename)

`java -jar ombibot.jar -p full/path/to/bot.env`
	
https://hub.docker.com/r/stacktraceyo/ombi-bot
