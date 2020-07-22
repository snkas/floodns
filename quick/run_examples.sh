cd ../simulator || exit 1

for experiment in "example_single" "example_ring" "example_leaf_spine" "example_leaf_spine_servers" "example_fat_tree_k4_servers" "example_two_tors"
do
  rm -rf ../runs/${experiment}/logs_floodns
  mkdir ../runs/${experiment}/logs_floodns
  java -jar floodns-basic-sim.jar "../runs/${experiment}" 2>&1 | tee ../runs/${experiment}/logs_floodns/console.txt || exit 1
done
