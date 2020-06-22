cd ../runs || exit 1
rm -rf */logs_floodns
cd ../simulator || exit 1
rm -rf target
rm -f floodns-basic-sim.jar
mvn clean
