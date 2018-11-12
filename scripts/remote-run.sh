IGNITE_HOME=`pwd`/storage nohup java -Xms512m -XX:+UseG1GC -cp orgpad-standalone.jar orgpad.main > ./logs/orgpad.log 2>&1 &
