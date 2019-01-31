clear all;
close all; clc

%% 
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%                                          %
% Graph metrics based forwarding technique %
%                                          %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Costruzione GRAFO
cluster =41; % dimensione del cluster
k = randi([floor(cluster-cluster/2)],1,cluster);    %grado di rete (distr. gaussiana)

% VISUALIZZAZIONE DEL GRAFO INIZIALE
Graph = zeros(cluster);

for i = 1: length(k) 
    for j = 1:length(k)
       somma_graph_col = sum(Graph);
       somma_graph_row = sum(Graph,2);
        
        if somma_graph_row(i) < k(i) && somma_graph_col(j) < k(j)
        if somma_graph_row(i) < i && somma_graph_col(j) < j
            if i == j 
               Graph(i,j) =0;
           else
               Graph(i,j) = 1;
           end
        end
        if somma_graph_row(i) == i && somma_graph_col(j) == j
           if i == j 
               Graph(i,j) =0;
           else
               Graph(i,j) = 1;
           end
        end
    end
    end
end

check = somma_graph_row' - k;
check_1 = somma_graph_col - k;
grado_rete = sum(Graph);

figure;
plot(graph(Graph),'NodeColor','k','MarkerSize',8');
title(['Initial graph with a cluster of ' num2str(cluster) ' nodes. Average degree = ' num2str(mean(grado_rete))])
set(gca,'FontSize',16,'FontName','Times New Roman')
set(get(gca, 'xlabel'), 'FontName', 'Times New Roman');
set(get(gca, 'xlabel'), 'FontSize', 16);
set(get(gca, 'ylabel'), 'FontName', 'Times New Roman');
set(get(gca, 'ylabel'), 'FontSize', 16);

%% Calcolo delle distanze dei nodi 
distanze_G = distances(graph(Graph)); % distanze nel grafo iniziale
distanze_medie_G = sum(sum(distanze_G),2)/(2*length(Graph));

source_veh = randi(cluster);
for gg = 1:length(distanze_G)
    Dist_veh(gg) = distanze_G(gg,source_veh);
end
max_dist_veh = find(Dist_veh==max(Dist_veh));

figure;
stem(Dist_veh,'-k*');
ylabel('Distance'); xlabel('Nodes');grid on
set(gca,'FontSize',18,'FontName','Times New Roman')
set(get(gca, 'xlabel'), 'FontName', 'Times New Roman');
set(get(gca, 'xlabel'), 'FontSize', 18);
set(get(gca, 'ylabel'), 'FontName', 'Times New Roman');
set(get(gca, 'ylabel'), 'FontSize', 18);

%% Calcolo coefficiente BRC

distanze_G = distances(graph(Graph)); 
g_new = zeros (cluster);
for i = 1:length(grado_rete)
    for j = 1:length(grado_rete)
        if distanze_G(i,j)==1
            g_new(i,j)=grado_rete(j);
        end
    end
end

g_new_inv = 1./g_new;
for i = 1:length(g_new_inv)
    for j = 1:length(g_new_inv)
        if g_new_inv(i,j) == Inf
            g_new_inv(i,j) = 0;
        end
    end
end

g_tot_vicini = sum(g_new_inv,2);

g_tot_vicini_riga = g_tot_vicini';

%grado_rete_inv = 1./grado_rete;

%coeff_BRC = grado_rete_inv./g_tot_vicini_riga;
coeff_BRC = grado_rete./g_tot_vicini_riga;

for i = 1:length(coeff_BRC)
    if coeff_BRC(i) == Inf
        coeff_BRC(i)=0;
    end
end


%% Calcolo della betweenness
Centr_between = centrality(graph(Graph),'betweenness')';
Avg_Centr_between = mean(Centr_between);

figure;
stem(Centr_between,'-k*');grid on
ylabel('Betweenness'); xlabel('Nodes')
set(gca,'FontSize',18,'FontName','Times New Roman')
set(get(gca, 'xlabel'), 'FontName', 'Times New Roman');
set(get(gca, 'xlabel'), 'FontSize', 18);
set(get(gca, 'ylabel'), 'FontName', 'Times New Roman');
set(get(gca, 'ylabel'), 'FontSize', 18);

%% GAME
argo_social_GAME = Centr_between./max(Centr_between);
P_S_GAME = log2(1+argo_social_GAME);
P_S_GAME_maximized = (P_S_GAME./max(P_S_GAME));
% 
figure;          % Probabilita' di social vehicle in base alla betweenness
plot(argo_social_GAME, P_S_GAME_maximized, 'ob'); grid on
xlabel('Betweenness')
ylabel('GAME vehicular social degree')
set(gca,'FontSize',18,'FontName','Times New Roman')
set(get(gca, 'xlabel'), 'FontName', 'Times New Roman');
set(get(gca, 'xlabel'), 'FontSize', 18);
set(get(gca, 'ylabel'), 'FontName', 'Times New Roman');
set(get(gca, 'ylabel'), 'FontSize', 18);

P_S_GAME_new = P_S_GAME_maximized;

%% Bridging Centrality


BRC = Centr_between.*coeff_BRC;
%BRC = coeff_BRC;

% BRC normalizzata 

argo_social_BRC = BRC./max(BRC);

figure;
stem(argo_social_BRC,'-k*');grid on
ylabel('Bridging Centrality'); xlabel('Nodes')
set(gca,'FontSize',18,'FontName','Times New Roman')
set(get(gca, 'xlabel'), 'FontName', 'Times New Roman');
set(get(gca, 'xlabel'), 'FontSize', 18);
set(get(gca, 'ylabel'), 'FontName', 'Times New Roman');
set(get(gca, 'ylabel'), 'FontSize', 18);

% Calcolo del grado di socialità in base alla Bridging Centrality

P_BRC= log2(1+ argo_social_BRC);

figure;          % Probabilita' di social vehicle in base alla Bridging
plot(argo_social_BRC, P_BRC, 'or'); grid on
xlabel('Bridging Centrality')
ylabel('Probability Bridging')
set(gca,'FontSize',18,'FontName','Times New Roman')
set(get(gca, 'xlabel'), 'FontName', 'Times New Roman');
set(get(gca, 'xlabel'), 'FontSize', 18);
set(get(gca, 'ylabel'), 'FontName', 'Times New Roman');
set(get(gca, 'ylabel'), 'FontSize', 18);

%% Calcolo dell'avg. clustering coefficient 
[ACC, LCC] = avgClusteringCoefficient(Graph);
%% Calcolo del grado di socialita' in base al Local clustering coefficient e alla betweenness (SWORDFISH)
argo_social_SWORDFISH = (LCC'.*Centr_between)./(LCC'+Centr_between);

argo_nuova = LCC' .* grado_rete./max(grado_rete);
for l = 1: (cluster)   
    P_S_SWORDFISH(l) = log2(1+argo_social_SWORDFISH(l)); % nuova versione 
    P_S_LCC_nuova(l) = log2(1+argo_nuova(l));
end
P_S_SWORDFISH_maximized = P_S_SWORDFISH./max(P_S_SWORDFISH);
P_S_LCC_nuova_maximized = P_S_LCC_nuova./max(P_S_LCC_nuova);

figure;
plot(argo_social_SWORDFISH, P_S_SWORDFISH,'ok');grid on
xlabel('{\it \lambda}'); ylabel('SWORDFISH vehicular social degree');
set(gca,'FontSize',16,'FontName','Times New Roman')
set(get(gca, 'xlabel'), 'FontName', 'Times New Roman');
set(get(gca, 'xlabel'), 'FontSize', 16);
set(get(gca, 'ylabel'), 'FontName', 'Times New Roman');
set(get(gca, 'ylabel'), 'FontSize', 16);




%% Comparison 
figure;
plot(argo_social_BRC, P_BRC, 'or'); 
figure;
plot(argo_social_GAME, P_S_GAME, 'ob'); 
figure;
plot(argo_social_SWORDFISH, P_S_SWORDFISH, 'ok'); 


%% Eliminazione nodo Sorgente
for l = 1 :length(P_S_SWORDFISH)
    if l == source_veh
        P_S_SWORDFISH_maximized(l) = 0;
        P_S_GAME_maximized(l) = 0;
        P_BRC(l) = 0;
    end   
end

%% Forwarding probability with social degree
z = 200;     % transmission range [m]

d1 = 50; d2 = 100; d3 = 150; d4 = 190;% distance inside the transmission range [m]


rho = 0.02;  
d=[0:2:200];            % transmission range [m]


s_factor = 1;
for l = 1:length(d)   
    for i = 1:length(P_BRC)
         Pf_d_BRC(l,i) = exp(-rho *(z-d(l))./(s_factor.*P_BRC(i))); % Prob. di forwarding SWORDFISH
         Pf_d_SWORDFISH(l,i) = exp(-rho *(z-d(l))./(s_factor.*P_S_SWORDFISH_maximized(i))); % Prob. di forwarding SWORDFISH
         Pf_d_GAME(l,i) = exp(-rho * (z-d(l))./(s_factor.*P_S_GAME_maximized(i)));
    end
end


 
%% GRAFICI
Pf_d_BRC_inverted  = Pf_d_BRC';
Pf_d_GAME_inverted  = Pf_d_GAME';
Pf_d_SWORDFISH_inverted  = Pf_d_SWORDFISH';

Dist_veh_1H  = find(Dist_veh ==1);

figure;
for i = 1:length(Dist_veh_1H)
    plot(d,Pf_d_BRC_inverted(Dist_veh_1H(i),:),'-^r'); hold on
    %plot(d,Pf_d_GAME_inverted(Dist_veh_1H(i),:),'-ob'); hold on   
    %plot(d,Pf_d_SWORDFISH_inverted(Dist_veh_1H(i),:),'-sk'); 
end
figure;
for i = 1:length(Dist_veh_1H)
    %plot(d,Pf_d_BRC_inverted(Dist_veh_1H(i),:),'-^r'); hold on
    plot(d,Pf_d_GAME_inverted(Dist_veh_1H(i),:),'-ob'); hold on
    %plot(d,Pf_d_SWORDFISH_inverted(Dist_veh_1H(i),:),'-sk');   
end
figure;
for i = 1:length(Dist_veh_1H)
    %plot(d,Pf_d_BRC_inverted(Dist_veh_1H(i),:),'-^r'); hold on
    %plot(d,Pf_d_GAME_inverted(Dist_veh_1H(i),:),'-ob'); hold on   
    plot(d,Pf_d_SWORDFISH_inverted(Dist_veh_1H(i),:),'-sk'); hold on
end
figure;
for i = 1:length(Dist_veh_1H)
    plot(d,Pf_d_BRC_inverted(Dist_veh_1H(i),:),'-^r'); hold on
    plot(d,Pf_d_GAME_inverted(Dist_veh_1H(i),:),'-ob'); hold on   
    plot(d,Pf_d_SWORDFISH_inverted(Dist_veh_1H(i),:),'-sk'); hold on
end
