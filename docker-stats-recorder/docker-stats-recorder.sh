#!/bin/bash

package='Record Docker Stats'

# Variables
container_ids=;
container_ids_array=;
output_file=;
ps_output_file=;
output_dir="./";
interval=5
samples=1
command='docker stats --no-stream --format "{{.ID}},{{.Name}},{{.CPUPerc}},{{.MemPerc}},{{.MemUsage}},{{.NetIO}},{{.BlockIO}},{{.PIDs}}"'

redis_argument=;
redis_user=;
redis_password=;
redis_host=;
redis_port=;
redis_cli_output_file=;

function join_by { local d=$1; shift; local f=$1; shift; printf %s "$f" "${@/#/$d}"; }

run_init_assertions() {
	if [ -z "$container_ids" ]; then
		print_usage ;
	else
		IFS=',' read -ra container_ids_array <<< $container_ids

		for i in "${container_ids_array[@]}"; do
    		command+=" $i"
		done
	fi

	if [ ! -d "${output_dir}" ]; then
	  mkdir "$output_dir"
	fi

	join_containers_name=$(join_by "_" ${container_ids_array[@]})
	output_file="$join_containers_name-stats.csv"
	ps_output_file="$join_containers_name-ps-stats.csv"

	if [ ! -f "${output_file}" ] || [ ! -s "${output_file}" ]; then 
		echo "container_id,container_name,cpu_perc,mem_perc,mem_usage,net_io,block_io,pids,timestamp" > "$output_dir/$output_file"
	fi

	if [ ! -f "${ps_output_file}" ] || [ ! -s "${ps_output_file}" ]; then
		echo "timestamp,container_id,pid,vsz,rss,comm" > "$output_dir/$ps_output_file"
	fi


  if [ ! -z "$redis_argument" ]; then
    IFS='@' read -ra redis_options <<< "$redis_argument"
    IFS=':' read -ra redis_user_pwd <<< "${redis_options[0]}"
    IFS=':' read -ra redis_host_port <<< "${redis_options[1]}"

    redis_user=${redis_user_pwd[0]}
    redis_password=${redis_user_pwd[1]}
    redis_host=${redis_host_port[0]}
    redis_port=${redis_host_port[1]}
  fi


  redis_cli_output_file="redis_info_stats.csv"
	if [ ! -f "${redis_cli_output_file}" ] || [ ! -s "${redis_cli_output_file}" ]; then
		echo "timestamp,used_memory,used_memory_rss,used_memory_dataset" > "$output_dir/$redis_cli_output_file"
	fi

	if [ $samples == -1 ]; then 
		echo "--- Recording infinite samples with $interval seconds between them ---"
	else
		echo "--- Recording $samples samples with $interval seconds between them ---"
	fi
}

print_usage() {
  echo "$package"
  echo " "
  echo "usage: $ docker-stats-recorder -c <container_id> [options]"
  echo " "
  echo "options:"
  echo "-h                                Show help (this screen)"
  echo "-c <container_id>,<container_id>  Indicate container to record"
  echo "-o <output_dir>                   Indicate directory where stats will be stored"
  echo "-i <interval seconds>             Interval between samples"
  echo "-s <number of samples>            Number of samples to recover. Set -1 to infinite"
  echo "-r <user:password@host:port>      Redis host to recover memory information"

  exit 0
}

process_redis_cli_command() {
  timestamp=$1

  redis_cli_command="redis-cli -h $redis_host -p $redis_port --tls --cacert thesis-prod-ssl/ca/thesis-ca.crt --key thesis-prod-ssl/redis-client/thesis-redis-cli.key --cert thesis-prod-ssl/redis-client/thesis-redis-cli.crt --user $redis_user -a '$redis_password' -c info memory"
  redis_cli_output=$(eval "$redis_cli_command")
  redis_cli_output=$(echo "$redis_cli_output" | tr -d '\r')

  while IFS= read -r line; do
    if [[ "$line" == used_memory:* ]]; then
      IFS=':' read -ra output_lin_separated <<< "$line"
      used_memory=${output_lin_separated[1]}
    fi

    if [[ "$line" == used_memory_rss:* ]]; then
      IFS=':' read -ra output_lin_separated <<< "$line"
      used_memory_rss="${output_lin_separated[1]}"
    fi

    if [[ "$line" == used_memory_dataset:* ]]; then
      IFS=':' read -ra output_lin_separated <<< "$line"
      used_memory_dataset="${output_lin_separated[1]}"
    fi
  done <<< "$redis_cli_output"

	echo "$timestamp,$used_memory,$used_memory_rss,$used_memory_dataset" >> "$output_dir/$redis_cli_output_file"
}

while getopts c:o:i:s:r: option; do
	case "${option}" in
		h) print_usage ;;
		c) container_ids=${OPTARG};;
		o) output_dir=${OPTARG};;
		i) interval=${OPTARG};;
		s) samples=${OPTARG};;
    r) redis_argument=${OPTARG};;
		*) print_usage ;;
	esac
done

run_init_assertions ;

index=0;
while [ $index -lt $samples ] || [ $samples == -1 ]; do
	echo "Recording a sample..."

	timestamp=$(date +%s%N)

	for i in "${container_ids_array[@]}"; do
		ps_command="docker exec $i ps -o pid,vsz,rss,comm | grep -v 'sh\|ps\|grep\|PID'"
		eval $ps_command | while IFS= read -r output_line ; do
			IFS=' ' read -ra output_lin_separated <<< $output_line
			ps_csv_line=$(join_by "," ${output_lin_separated[@]})

			echo "$timestamp,$i,$ps_csv_line" >> "$output_dir/$ps_output_file"
		done
	done

	eval $command | while IFS= read -r output_line ; do
		output_line+=",$timestamp"
		echo "$output_line" >> "$output_dir/$output_file"
	done

  if [ ! -z "$redis_argument" ]; then
    process_redis_cli_command $timestamp ;
  fi
	
	((index++))

	if [ $index -lt $samples ] || [ $samples == -1 ]; then
		sleep "$interval"
	fi
done
