#!/bin/bash

package='Record Docker Start Up Times'

# Variables
name=;
ports=;
device=;
environment=;
image=;

url=;

docker_command="docker run --rm -d"
date_command="date +%s%N";
initial_time=;
final_time=;
startup_time=;

docker_container_id=;

function join_by { local d=$1; shift; local f=$1; shift; printf %s "$f" "${@/#/$d}"; }

print_usage() {
  echo "$package"
  echo " "
  echo "usage: $ docker-startup-time-recorder -i <image> -u <health_url> [options]"
  echo " "
  echo "options:"
  echo "-h                      			Show help (this screen)"
  echo "-m                            Set -m option if running on macos"
  echo "-n <name>                     Container name"
  echo "-p <port:port>                Container ports. Repeatable"
  echo "-e <environment>              Container environment variable. Repeatable"
  echo "-d <device>                   Container device mount"
  echo "-i <image:tag>                Container image to run"
  echo "-u <protocol://host:port/url> Service health url"

  exit 0
}

run_init_assertions() {
  if [ -z "$image" ] || [ -z "$url" ]; then
    print_usage ;
  fi
}

build_docker_command () {
  if [ -n "$name" ]; then
    docker_command+=" --name $name" ;
  fi

  if [ -n "$device" ]; then
    docker_command+=" --device=$device" ;
  fi

  if [ ${#ports[@]} -gt 1 ]; then
    ports_command=$(join_by " -p " ${ports[@]})
    docker_command+=" -p $ports_command"
  fi

  if [ ${#environment[@]} -gt 1 ]; then
    environment_command=$(join_by " -e " ${environment[@]})
    docker_command+=" -e $environment_command"
  fi

  docker_command+=" $image"
}

trapSigInt () {
  printf "\rCleaning up and exiting...\n"
  eval "docker stop $docker_container_id"
  echo "Done!"
  exit 0
}

trap trapSigInt SIGINT

while getopts 'p:d:e:n:i:u:hm' option; do
  case "${option}" in
    h) print_usage ;;
    m) date_command="gdate +%s%N";;
    n) name=${OPTARG} ;;
    p) ports+=(${OPTARG}) ;;
    d) device=${OPTARG} ;;
    e) environment+=(${OPTARG}) ;;
    i) image=${OPTARG} ;;
    u) url=${OPTARG} ;;
    *) print_usage ;;
  esac
done

run_init_assertions ;

build_docker_command ;

# eval $docker_command
docker_container_id=$(eval "$docker_command")
echo "$docker_container_id"
echo "Waiting for the container to get ready..."

initial_time=$($date_command);

until $(curl -k --output /dev/null --silent --fail $url); do
	:
done

final_time=$($date_command);
startup_time=$(($final_time - initial_time))

# echo $time
echo "Container took $startup_time ns to become ready."

echo "Stopping and removing container..."
eval "docker stop $docker_container_id"
echo "Done!"
