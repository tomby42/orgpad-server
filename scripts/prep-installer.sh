sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get --yes --force-yes install oracle-java8-installer
mkdir bin
cd bin
wget -X GET https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod a+x lein
lein
