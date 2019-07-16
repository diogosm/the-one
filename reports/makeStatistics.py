#!/ur/bin/env python
# -*- coding: utf-8 -*-
# @author: Diogo 'dsm' Soares
#
import fnmatch
import mmap
import numpy
import pandas
import os
import math

'''
    Main parameters depending of network trace used
    :param dir: Directory name where results are (reports/$dir)
        values: Sassy or Infocom5
    :param betweennessFile:
    :param lccFile: 
'''
dir = "Sassy"
betweennessFile = "BetweennesseachNode.csv"
lccFile = "LCCeachNode.csv"
simulSettings = "../settings" + dir + ".txt"

class Estatisticas:
    """
        :param overallEnergyTime: DataFrame pros dados estatisticos da energia
            inclui:
                - tempo
                - media
                - desvio padrao
                - min CI
                - max CI
    """
    def __init__(self):
        self.overallEnergyTime = []

class Node:
    def __init__(self, id):
        self.id = id
        self.lcc = 0.0
        self.betweenness = 0.0
        self.energiaTempo = {}
        self.centralidade = 0.0  # cWindow = 6 hours
        self.qtdContatos = 0
        self.tempoContato = 0.0  # tempo médio de contato

    def __getId__(self):
        return self.id

    def __getLcc__(self):
        return self.lcc

    def __getBetweenness__(self):
        return self.betweenness

    def __getCentralidade__(self):
        return self.centralidade

    def __getQtdContatos__(self):
        return self.qtdContatos

    def __getTempoContato__(self):
        return self.tempoContato

'''
    @TODO
        - remove tipo
'''
def fazHeatMap(simulTime, nodes, estatisticas, tipo):
    x = []
    y = []
    z = []
    yAux = []

    for i in range(len(nodes)):
        '''
        if tipo == "LCC":
            yAux.append((nodes[i].__getLcc__(), nodes[i].__getId__()))
        elif tipo == "Betweenness":  # Betweenness
            yAux.append((nodes[i].__getBetweenness__(), nodes[i].__getId__()))
        else:
            yAux.append((nodes[i].__getCentralidade__(), nodes[i].__getId__()))
        '''
        yAux.append(
            (nodes[i].__getCentralidade__(),
             nodes[i].__getId__(),
             nodes[i].__getLcc__(),
             nodes[i].__getBetweenness__(),
             nodes[i].__getQtdContatos__(),
             nodes[i].__getTempoContato__())
        )
    yAux.sort()

    for tupla in yAux:
        x.append(tupla.__getitem__(1))
        y.append(tupla.__getitem__(0))

    ans = estatisticas.overallEnergyTime.loc[
        estatisticas.overallEnergyTime[0] == simulTime
        ]

    for energia in ans[1]:
        z.append(energia)

    x = numpy.asarray(x)
    y = numpy.asarray(y)
    z = numpy.asarray(z)
    yAux = numpy.asarray(yAux)

    df = pandas.DataFrame.from_dict(
        numpy.array([x, y, yAux[:, 2], yAux[:, 3], yAux[:, 4], yAux[:, 5], z]).T
    )
    df.columns = ['Node ID',
                  'Centralidade',
                  'LCC',
                  'Betweenness',
                  'Contacts Qty',
                  'Mean Contact Time',
                  'Residual Energy']
    df['Residual Energy'] = pandas.to_numeric(df['Residual Energy'])

    #pivotted = df.pivot('Node ID', 'Centralidade', 'LCC', 'Residual Energy')

    return df


def getQtdContatos(trace, node):
    traceAux = trace[trace['type'] == 'UP']
    traceFromNode = traceAux[traceAux['from'] == node]
    traceToNode = traceAux[traceAux['to'] == node]

    if dir == "Infocom5":
        return (traceFromNode.count()[0] + traceToNode.count()[0])/2.0
    else:
        return traceFromNode.count()[0] + traceToNode.count()[0]

def getTempoMedioContatos(trace, node):
    tempos = []
    if dir == "Infocom5":    ## trace esta unidirecional
        traceAux = trace[trace['from'] == node]
        traceAux = traceAux.sort_values(['from', 'to', 'time', 'type'], ascending=[True, True, True, False])

        for index, row in traceAux.iterrows():
            if row['type'] == 'DOWN':
                tempo = row['time'] - rowBefore['time']
                tempos.append(tempo)
            rowBefore = row
        tempos = numpy.asarray(tempos)
        return tempos.mean()
    '''
        traces diferentes do Infocom5
        trace bidireciponal (Rollernet e Sassy, por exemplo)
    '''
    traceAux = trace[(trace['from'] == node) | (trace['to'] == node)]
    vizinhos = []
    for vizinho in range(numHosts):
        vizinhos.append(-1)

    for index, row in traceAux.iterrows():
        if row['from'] == node:
            para = row['to']
        else:
            para = row['from']

        if row['type'] == "UP" and vizinhos[para] > -1:
            continue
        if row['type'] == "DOWN" and vizinhos[para] < 0:
            continue

        if row['type'] == "UP" and vizinhos[para] < 0:
            vizinhos[para] = row['time']
        elif row['type'] == "DOWN" and vizinhos[para] > -1: ## fim da conexao entre eles
            tempoDescorrido = row['time'] - vizinhos[para]
            vizinhos[para] = -1
            tempos.append(tempoDescorrido)

    tempos = numpy.asarray(tempos)
    return tempos.mean()

if __name__ == "__main__":
    arq = open(simulSettings)
    mapeado = mmap.mmap(arq.fileno(), 0, access=mmap.ACCESS_READ)
    mapeado.seek(mapeado.find('Group.nrofHosts'))
    numHosts = int(mapeado.readline().split(" = ")[1].replace("\n", ""))
    lccData = pandas.read_csv(dir + "/resultados/" + lccFile, sep=" ", header=None)
    betweennessData = pandas.read_csv(dir + "/resultados/" + betweennessFile, sep=" ", header=None)
    mapeado.seek(mapeado.find('Events2.filePath'))
    traceLocalization = str(mapeado.readline().split(" = ")[1].replace("\n", ""))
    trace = pandas.read_csv('../' + traceLocalization, sep=" ", header=None)
    trace.columns = ['time', 'trash', 'from', 'to', 'type']
    ## converte type uppercase
    trace['type'] = map(lambda x: x.upper(), trace['type'])

    nodes = [Node(i) for i in range(numHosts)]
    estatisticas = Estatisticas()

    for i in range(numHosts):
        nodes[i].lcc = lccData[2][i]
        nodes[i].betweenness = betweennessData[2][i]
        nodes[i].qtdContatos = getQtdContatos(trace, i)
        nodes[i].tempoContato = getTempoMedioContatos(trace, i)

    dadosCentralidade = pandas.read_csv("WiMob/centralidade" + dir + ".csv", encoding='utf-8', delimiter=';')
    for i in range(numHosts):
        nodes[i].centralidade = dadosCentralidade['Centrality'][i]

    # read energy level by time
    for fileName in os.listdir(dir):
        if fnmatch.fnmatch(fileName, "*EnergyLevel*.txt"):
            arq = open(dir + "/" + fileName)
            linhas = arq.readlines()
            tempo = -1
            for linha in linhas:
                if linha.find("[") != -1:
                    linha = linha.replace("[", "")
                    tempo = linha = int(linha.replace("]", ""))
                else:
                    nodeId, energy = linha.split(" ")
                    nodeId = int(nodeId.replace("p", ""))
                    energy = float(energy.replace("\n", ""))
                    if tempo in nodes[nodeId].energiaTempo:
                        nodes[nodeId].energiaTempo[tempo].append(energy)
                    else:
                        nodes[nodeId].energiaTempo[tempo] = []
                        nodes[nodeId].energiaTempo[tempo].append(energy)

    for i in range(numHosts):
        ## overall energy by time
        for time in sorted(nodes[i].energiaTempo):
            linha = []
            timeEnergy = 0.0
            media = 0.0
            desvioPadrao = 0.0

            for energy in nodes[i].energiaTempo[time]:
                timeEnergy += energy
            media = timeEnergy / (1. * len(nodes[i].energiaTempo[time]))
            for energy in nodes[i].energiaTempo[time]:
                aux = energy - media
                aux *= aux
                desvioPadrao += aux
            desvioPadrao = math.sqrt(desvioPadrao /
                                     (1. * len(nodes[i].energiaTempo[time]))
                                     )
            ## 1.960 = 95% CI
            minCI = media - (1.960 *
                             math.sqrt(desvioPadrao /
                                       (1. * len(nodes[i].energiaTempo[time])))
                             )
            maxCI = media + (1.960 *
                             math.sqrt(desvioPadrao /
                                       (1. * len(nodes[i].energiaTempo[time])))
                             )

            linha.append(time)
            linha.append(media)
            linha.append(desvioPadrao)
            linha.append(minCI)
            linha.append(maxCI)
            estatisticas.overallEnergyTime.append(linha)

    estatisticas.overallEnergyTime = pandas.DataFrame(data=estatisticas.overallEnergyTime)
    estatisticas.overallEnergyTime.to_csv("Resultado_Energia_"+dir, header=False)

    # 274800 = tempo final da simulacao (sassy)
    # 86400 = tempo final da simulacao (infocom5)
    if dir == "Sassy":
        #  fazHeatMap(274800, nodes, estatisticas, tipo="LCC")
        ans = fazHeatMap(777600, nodes, estatisticas, tipo="Centrality")
    elif dir == "Infocom5":
        #  fazHeatMap(86400, nodes, estatisticas, tipo="Betweenness")
        ans = fazHeatMap(86400, nodes, estatisticas, tipo="Centrality")
    elif dir == "Rollernet":
        ans = fazHeatMap(9600, nodes, estatisticas, tipo="Centrality")

    #  seaborn.heatmap(ans, cmap='RdBu')
    #  matplotlib.pylab.show()

    arq.close()
    print '---------------[FINISHED]---------------'
