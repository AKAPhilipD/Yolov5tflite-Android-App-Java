from ultralytics import YOLO
# 加载训练好的模型，改为自己的路径
model = YOLO('D:/dsh AI/1221001007-董士赫-yolo11app/ultralytics-main/runs/detect/train4/weights/best.pt')
sources = ['D:/dsh AI/1221001007-董士赫-yolo11app/test_data/1.jpg']#'D:/dsh AI/1221001007-董士赫-yolo11app/test_data/2.jpg'#]
# 运行推理，并附加参数
for source in sources:
 model.predict(source, save=True)
