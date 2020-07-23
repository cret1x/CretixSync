from database import db_session
from flask_api import status
from colorama import init as colorama_init
from flask import *
import models
import hashlib
import os
import socket
import json
import codecs
from zlib import adler32

NO_ERROR = 0
ERROR_NO_DATA = 1
ERROR_WRONG_PASS = 2
ERROR_WRONG_FNAME = 3

colorama_init()

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.connect(("8.8.8.8", 80))



app = Flask(__name__)

app.config['localIp'] = s.getsockname()[0]
s.close()


def md5(password):
	return hashlib.md5(password.encode()).hexdigest()


@app.route('/')
def home():
	groups = models.Group.query.all()
	return render_template('index.html', groups = groups)

@app.route('/groups/<int:gid>')
def group(gid):
	devices = models.Device.query.filter_by(group_id = gid).all()
	return render_template('group.html', devices = devices)


@app.route('/upload/<gid>/<login>/<album>/<filename>', methods=['POST'])
def upload(gid,login,album,filename):
	file = request.files['file']
	path = models.Group.query.filter_by(id = gid).first().path
	path = os.path.join(path, login)
	path = os.path.join(path, album)
	with open(os.path.join(path, filename), 'wb') as fp:
		fp.write(file.read())

	return jsonify({'status': 201}), 201


@app.route('/create-group', methods = ['POST'])
def createGroup():
	name = request.form.get('name')
	password = request.form.get('password')
	path = request.form.get('path')
	if not name or not password or not path:
		return redirect('/')

	try:
		os.makedirs(path)
	except:
		pass
	
	group = models.Group(name = name, password = md5(password), path = path)
	db_session.add(group)
	db_session.commit()
	return redirect('/')

@app.route('/register-device', methods = ['POST'])
def createDevice():
	content = request.get_json()
	if not content:
		return jsonify({'status': 400, 'error': ERROR_NO_DATA}), 400
	login = content.get('login')
	password = content.get('password')
	ip = request.remote_addr
	if not login or not password:
		return jsonify({'status': 400, 'error': ERROR_NO_DATA}), 400

	master_group = models.Group.query.filter_by(password = md5(password)).first()
	if not master_group:
		return jsonify({'status': 400, 'error': ERROR_WRONG_PASS}), 400

	try:
		os.makedirs(os.path.join(master_group.path, login))
	except:
		pass
	device = models.Device(login = login, owner = master_group, last_ip = ip, last_status = 'online')
	db_session.add(device)
	db_session.commit()
	return jsonify({'status': 201, 'error': NO_ERROR}), 201

@app.route('/login-device', methods = ['POST'])
def loginDevice():
	content = request.get_json()
	if not content:
		return jsonify({'status': 400, 'error': ERROR_NO_DATA}), 400
	login = content.get('login')
	password = content.get('password')
	ip = request.remote_addr
	if not login or not password:
		return jsonify({'status': 400, 'error': ERROR_NO_DATA}), 400

	master_group = models.Group.query.filter_by(password = md5(password)).first()
	if not master_group:
		return jsonify({'status': 400, 'error': ERROR_WRONG_PASS}), 400

	device = models.Device.query.filter_by(login=login).update({'last_ip': ip})
	db_session.commit()
	return jsonify({'status': 201, 'error': NO_ERROR}), 201


@app.route('/start-sync', methods = ['POST'])
def startSync():
	
	content = request.get_json()
	if not content:
		return jsonify({'status': 400, 'error': ERROR_NO_DATA}), 400

	albums = content.get('albums')
	login = content.get('login')
	password = content.get('password')

	device = models.Device.query.filter_by(login=login).first()
	if not device:
		return jsonify({'status': 400, 'error': ERROR_WRONG_PASS}), 400
	group = models.Group.query.filter_by(id = device.group_id).first()
	if not group:
		return jsonify({'status': 400, 'error': ERROR_WRONG_PASS}), 400
	if group.password != md5(password):
		return jsonify({'status': 400, 'error': ERROR_WRONG_PASS}), 400
	# albums -> create folders
	if len(albums) == 0:
		return jsonify({'status': 400, 'error': ERROR_NO_DATA}), 400

	base_path = os.path.join(group.path, login)
	for album in albums:
		path = os.path.join(base_path, str(adler32(album.encode())))
		try:
			os.makedirs(path)	
			fs = codecs.open(os.path.join(path,'info.json'), 'w', encoding='utf-8')
			json.dump({'name': album}, fs)
			fs.close()
		except Exception as e:
			print(e)
	# return link for upload
	url = '/upload/{}'.format(device.group_id)
	return jsonify({'status': 200, 'url': url})

@app.route('/pulse', methods = ['POST'])
def pulse():
	content = request.get_json()
	login = content.get('login')
	password = content.get('password')
	ip = request.remote_addr

	device = models.Device.query.filter_by(login=login).update({'last_ip': ip})
	db_session.commit()
	return jsonify({'status': 200})

@app.route('/info', methods = ['POST'])
def getInfo():
	content = request.get_json()
	login = content.get('login')
	password = content.get('password')
	master_group = models.Group.query.filter_by(password = md5(password)).first()
	return jsonify({'status': 200, 'name': master_group.name})
    
@app.teardown_appcontext
def shutdown_session(exception=None):
    db_session.remove()


if __name__ == '__main__':
	app.run(host = '0.0.0.0', port = 5000, debug = True)
	