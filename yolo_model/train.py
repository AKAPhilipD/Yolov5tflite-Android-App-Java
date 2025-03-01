from ultralytics import YOLO
if __name__ == '__main__':
    model = YOLO('yolov5.yaml')  # 从YAML建立一个新模型
    model.load('yolov5n.pt')
    # # 训练模型/
    results = model.train(data='ig_team.yaml',
                      epochs=200, imgsz=320, device="0", optimizer='SGD', workers=8, batch=64, amp=False)

