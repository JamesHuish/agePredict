import flask
from flask import request
import werkzeug
from keras.models import load_model
import cv2
import numpy as np
import coral_ordinal as coral
from tf_keras_vis.utils import normalize
from tf_keras_vis.scorecam import Scorecam
from tf_keras_vis.gradcam import Gradcam
import matplotlib.pyplot as plt
import shutil
import subprocess
import os
from tf_keras_vis.saliency import Saliency
from tf_keras_vis.utils.model_modifiers import ReplaceToLinear

app = flask.Flask(__name__)
#run_with_ngrok(app)

model = load_model("D:/models/FINAL/Mobile/Testing/strat-cross-val2.h5")

@app.route('/predict', methods = ['POST'])
def predict():
    imagefile = flask.request.files['image']
    filename = werkzeug.utils.secure_filename(imagefile.filename)
    print("\nReceived image File name : " + imagefile.filename)
    imagefile.save(filename)

    image = cv2.imread(filename)
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    image = cv2.resize(image, (224, 224))
    image = np.expand_dims(image, axis=0)
    image = image /255
    
    prediction = model.predict(image)

    output_indexes = np.array([i for i in range(101)])
    probs = coral.ordinal_softmax(prediction).numpy()
    predicted_age = np.sum(probs * output_indexes,axis = 1)

    return "Age Prediction: " + str(predicted_age[0].round(2))

@app.route('/heatmap', methods=['POST'])
def heatmap():
    plt.switch_backend('agg')
    req = request.data.decode('utf-8')
    image = cv2.imread("androidFlask.jpg")
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    image = cv2.resize(image, (224, 224))
    image = np.expand_dims(image, axis=0)
    image = image /255


    def score_function(output):
        return output[:, 0]
    print(req)
    if req == 'Saliency Map':
        replace2linear = ReplaceToLinear()

        saliency = Saliency(model,
                    model_modifier=replace2linear,
                    clone=True)

        saliency_map = saliency(score_function, image)
        f, ax = plt.subplots(nrows=1, ncols=1, figsize=(8, 4))
        ax.imshow(image[0], cmap='gray')
        ax.imshow(saliency_map[0], cmap='jet',alpha=0.5)
        ax.axis('off')
        plt.subplots_adjust(left=0, right=1, top=1, bottom=0)
        plt.savefig("vis.png", bbox_inches='tight', transparent=True, pad_inches=0)
        plt.close()
    elif req == 'Smooth Saliency Map':
        replace2linear = ReplaceToLinear()

        saliency = Saliency(model,
                    model_modifier=replace2linear,
                    clone=True)
        saliency_map = saliency(score_function, image, smooth_samples=100,
                        smooth_noise=0.20)
        f, ax = plt.subplots(nrows=1, ncols=1, figsize=(8, 4))
        ax.imshow(image[0], cmap='gray')
        ax.imshow(saliency_map[0], cmap='jet', alpha=0.5)
        ax.axis('off')
        plt.subplots_adjust(left=0, right=1, top=1, bottom=0)
        plt.savefig("vis.png", bbox_inches='tight', transparent=True, pad_inches=0)
        plt.close()
    elif req == 'GradCam':
        scorecam = Gradcam(model)
        cam = scorecam(score_function, image, penultimate_layer=-1)
        cam = normalize(cam)
        f, ax = plt.subplots(nrows=1, ncols=1, figsize=(8, 4))
        ax.imshow(image[0], cmap='gray')
        ax.imshow(cam[0], cmap='jet', alpha=0.5)
        plt.savefig("vis.png", bbox_inches='tight', transparent=True, pad_inches=0)
        plt.close()
    else:
        scorecam = Scorecam(model)
        cam = scorecam(score_function, image, penultimate_layer=-1, max_N=10)
        cam = normalize(cam)
        inverted_cam = 1-cam
        f, ax = plt.subplots(nrows=1, ncols=1, figsize=(8, 4))
        ax.imshow(image[0], cmap='gray')
        ax.imshow(inverted_cam[0], cmap='jet', alpha=0.5)
        ax.axis('off')
        plt.subplots_adjust(left=0, right=1, top=1, bottom=0)
        plt.savefig("vis.png", bbox_inches='tight', transparent=True, pad_inches=0)
        plt.close()

    return flask.send_file("vis.png", mimetype='image/png')

@app.route('/ageChange', methods=['GET', 'POST'])
def ageChange():
    
    shutil.copy("androidFlask.jpg", "HRFAE-master/test/input")
    os.chdir("HRFAE-master/")
    subprocess.run(["python", "test.py", "--config", "001", "--target_age", "65"])
    """print(os.getcwd())
    os.chdir("test/output/")
    shutil.move("androidFlask_age_65.jpg", "../../../")"""
    os.chdir("../")
    return flask.send_file("HRFAE-master/test/output/androidFlask_age_65.jpg", mimetype='image/jpg')

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=True)