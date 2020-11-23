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
  echo "-h                      			Show help (this screen)"
  echo "-c <container_id>,<container_id>    Indicate container to record"
  echo "-o <output_dir>        			  Indicate directory where stats will be stored"
  echo "-f <csv>     	        			  Indicate the format of stored stats"
  echo "-i <interval seconds>   			Interval between samples"
  echo "-s <number of samples>  			Number of samples to recover. Set -1 to infinite"

  exit 0
}

while getopts c:o:i:s: option; do
	case "${option}" in
		h) print_usage ;;
		c) container_ids=${OPTARG};;
		o) output_dir=${OPTARG};;
		i) interval=${OPTARG};;
		s) samples=${OPTARG};;
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
	
	((index++))

	if [ $index -lt $samples ] || [ $samples == -1 ]; then
		sleep "$interval"
	fi
done
