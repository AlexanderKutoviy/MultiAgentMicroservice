# fetch basic image
FROM maven:3.3.9-jdk-8

# application placed into /opt/app
RUN mkdir -p /opt/app
WORKDIR /opt/app

## selectively add the POM file and
## install dependencies
#COPY pom.xml /opt/app/
#RUN mvn install
#
## rest of the project
#COPY src /opt/app/src
#RUN mvn package

COPY run_utils /opt/app/run_utils
COPY edu /opt/app/edu

# local application port
EXPOSE 4507

# execute it
#CMD ["mvn", "exec:java"] 
CMD java -cp _sandbox/m_agent_service-1.0.jar main.Application