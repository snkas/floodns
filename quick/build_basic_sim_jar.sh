cd ../simulator || exit 1
mvn compile assembly:single
mv target/floodns-*-jar-with-dependencies.jar floodns-basic-sim.jar
