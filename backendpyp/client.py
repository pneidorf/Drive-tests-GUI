import requests
import matplotlib.pyplot as plt

# Функция для получения данных
def get_communication_data():
    response = requests.get("http://localhost:8080/communication")
    data = response.json()
    return data

# Функция для построения графиков на основе полученных характеристик связи
def plot_graphs(communication_data):
    bandwidth = communication_data['bandwidth']
    latency = communication_data['latency']
    packet_loss = communication_data['packet_loss']

    # Построение графиков
    plt.figure(figsize=(10, 6))
    plt.subplot(1, 3, 1)
    plt.bar(['Bandwidth'], [bandwidth], color='blue')
    plt.title('Bandwidth (Mbps)')

    plt.subplot(1, 3, 2)
    plt.bar(['Latency'], [latency], color='orange')
    plt.title('Latency (ms)')

    plt.subplot(1, 3, 3)
    plt.bar(['Packet Loss'], [packet_loss], color='green')
    plt.title('Packet Loss (%)')

    plt.tight_layout()
    plt.show()

communication_data = get_communication_data()
plot_graphs(communication_data)