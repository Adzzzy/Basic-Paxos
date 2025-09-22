FROM eclipse-temurin:21-jdk-noble
#Use eclipse temurin for version 21 of the JDK built on Noble Ubuntu base image
#Eclipse temurin jdk 21 on Alpine final image is around 100MB smaller but has a severe vulnerability so will use Ubuntu for now

LABEL Maintainer="Adam Rebes <https://github.com/Adzzzy>"

#copy the contents of the host machine's present working directory into the image's pwd with COPY . .
COPY . .
#.dockerignore file has been created to specify files that will be ignored in this copy step to reduce overall image size

#compile Client using javac. Client file depends on other classes so they will be compiled automatically as well. Would have to specify the other files if they weren't in the same directory.
RUN javac Client.java

#Entrypoint commands given in Exec form i.e. ["executable", "param1", "param2"]
ENTRYPOINT [ "java", "Client" ]
#java Client to to run the Client java binary executable