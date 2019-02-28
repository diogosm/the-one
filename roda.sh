#!/bin/bash

./compile.sh

## apaga os logs
rm -rf reports/*

#for i in 1 2 3 4 5 6 7 8 9
#do
for ttl in 60 180 360 540 720 900 1080 1260 1440
do

	## roda MaxPropRouter
    #sed -i 's/^Group.router = .*/Group.router = MaxPropRouter/g' movimentoDeCarro.txt
    #sed -i 's/^Scenario.name = .*/Scenario.name = R=MaxPropRouter_N=40_T=12h_TTL='$ttl'_Seed=%%MovementModel.rngSeed%%/g' movimentoDeCarro.txt
    #sed -i 's/^Group.msgTtl = .*/Group.msgTtl = '$ttl'/g' movimentoDeCarro.txt

    ## roda
	#echo "########## RODANDO MaxPropRouter $ttl 9vezes ##########"
    #./one.sh -b 9 movimentoDeCarro.txt

    #mkdir -p reports/MaxPropRouter_N=40_T=12h_TTL="$ttl"
    #mv reports/*.txt reports/MaxPropRouter_N=40_T=12h_TTL="$ttl"

	## roda EPIDEMICO
	sed -i 's/^Group.router = .*/Group.router = EpidemicRouter/g' movimentoDeCarro.txt
	sed -i 's/^Scenario.name = .*/Scenario.name = R=Epidemic_N=40_T=12h_TTL='$ttl'_Seed=%%MovementModel.rngSeed%%/g' movimentoDeCarro.txt
	sed -i 's/^Group.msgTtl = .*/Group.msgTtl = '$ttl'/g' movimentoDeCarro.txt

	## roda
	echo "########## RODANDO EPIDEMICO $ttl 9vezes ##########"
	./one.sh -b 9 movimentoDeCarro.txt

	mkdir -p reports/Epidemic_N=40_T=12h_TTL="$ttl"
	mv reports/*.txt reports/Epidemic_N=40_T=12h_TTL="$ttl"

	## roda SWORDFISH
	sed -i 's/^Group.router = .*/Group.router = Swordfish/g' movimentoDeCarro.txt
	sed -i 's/^Scenario.name = .*/Scenario.name = R=Swordfish_N=40_T=12h_TTL='$ttl'_Seed=%%MovementModel.rngSeed%%/g' movimentoDeCarro.txt
	sed -i 's/^Group.msgTtl = .*/Group.msgTtl = '$ttl'/g' movimentoDeCarro.txt

	## roda
	echo "########## RODANDO SWORDFISH $ttl 9vezes ##########"
	./one.sh -b 9 movimentoDeCarro.txt

	mkdir -p reports/Swordfish_N=40_T=12h_TTL="$ttl"
	mv reports/*.txt reports/Swordfish_N=40_T=12h_TTL="$ttl"

	## roda GAME
	sed -i 's/^Group.router = .*/Group.router = Game/g' movimentoDeCarro.txt
	sed -i 's/^Scenario.name = .*/Scenario.name = R=Game_N=40_T=12h_TTL='$ttl'_Seed=%%MovementModel.rngSeed%%/g' movimentoDeCarro.txt
	sed -i 's/^Group.msgTtl = .*/Group.msgTtl = '$ttl'/g' movimentoDeCarro.txt

	## roda
	echo "########## RODANDO GAME $ttl 9vezes ##########"
	./one.sh -b 9 movimentoDeCarro.txt

	mkdir -p reports/Game_N=40_T=12h_TTL="$ttl"
	mv reports/*.txt reports/Game_N=40_T=12h_TTL="$ttl"

	## roda PROPHET
	sed -i 's/^Group.router = .*/Group.router = ProphetRouter/g' movimentoDeCarro.txt
	sed -i 's/^Scenario.name = .*/Scenario.name = R=ProphetRouter_N=40_T=12h_TTL='$ttl'_Seed=%%MovementModel.rngSeed%%/g' movimentoDeCarro.txt
	sed -i 's/^Group.msgTtl = .*/Group.msgTtl = '$ttl'/g' movimentoDeCarro.txt

	## roda
	echo "########## RODANDO ProphetRouter $ttl 9vezes ##########"
	./one.sh -b 9 movimentoDeCarro.txt

	mkdir -p reports/ProphetRouter_N=40_T=12h_TTL="$ttl"
	mv reports/*.txt reports/ProphetRouter_N=40_T=12h_TTL="$ttl"

done
#done


