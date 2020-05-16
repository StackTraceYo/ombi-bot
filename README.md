
# ombi-bot
![](https://img.shields.io/docker/pulls/stacktraceyo/ombi-bot.svg) 
<a href="https://www.buymeacoffee.com/stacktraceyo" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" ></a>

Telegram bot for Ombi Requests
- v2.1-SNAPSHOT


## commands:
* /searchmovie Movie Title or imdb url
* /searchtv TV show name or imdb url
* /search Search both Movie and TV show name or imdb url
* /info
* /authinfo

## auth commands: 
(admin can only run these)
* /register registers the current chat
* /unregister unregisters the current chat
* /unregisterall removes all chats
* /authorize <user id> authorizes a user to make requests
* /extauthorize <user id> authorizes a user to make requests in any chat (like private chat with bot)
* /unauthorize <user id> removes the user from allowed requesters
* /unauthorizeall removes all users other than the admin
* /authon enables authorization
* /authoff disables authorization (authorized users will stay authorized if toggled back on)



## authorization notes:

in your env file, if you provide the BOT_ADMIN env variable with a user id,
then authorization will be enabled on bot start, and that user id will be considered admin.
the admin will have to add users, to allow them to make request commands.

additionally, admin will have to register and unregister chats to use the bot

here is a table

              BOT_ADMIN    BOT_CHAT_ID    registration    auth       who_can_search
    defined   yes           yes            yes             yes       users in a registered chat and authorized, initialized with BOT_CHAT_ID
    defined   yes           no             yes             yes       users in a registered chat and authorized
    defined   no            yes            no              no        users in the chat from BOT_CHAT_ID
    defined   no            no             no              no        anyone

- if you do not have BOT_ADMIN AND have a BOT_CHAT_ID then everyone in that chat only will be allowed to use the bot.
- if BOT_ADMIN is not set, no authorization is done - and cannot be turned on.
- you can disable or enable the authorization support only if there is an admin.
- toggling using the /authon and /authoff commands does not clear the authorized users
- only the admin can use the authorization commands, meaning if you put it in wrong. you will be locked out and the bot 
  will not be usable.

you can get your user id from the @userinfobot.


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
BOT_ADMIN=<admin user id> (OPTIONAL) // admin user id - see authorization section
BOT_CHAT_ID=<allowed chat id> (OPTIONAL) //  - see authorization section 
```
* the run (bot.env is ex env filename)

##### with docker

available tags:  2.0 , 1.2, latest

for the latest changes (off master) run `docker run --env-file bot.env -d  stacktraceyo/ombi-bot:latest`

for 2.0 run `docker run --env-file bot.env -d  stacktraceyo/ombi-bot:2.0` 

for 1.X release run `docker run --env-file bot.env -d  stacktraceyo/ombi-bot:1.2` 


##### without docker

* install java version 8+ and maven
* clone project `git clone https://github.com/StackTraceYo/ombi-bot`
* cd into directory and run `mvn clean install`
* get the output jar at `ombi-bot/ombi-bot/target/ombibot.jar`
* run the jar directly with the env file (bot.env is ex env filename)

`java -jar ombibot.jar -p full/path/to/bot.env`
	
https://hub.docker.com/r/stacktraceyo/ombi-bot
