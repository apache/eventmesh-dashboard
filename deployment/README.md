## Auto Deploy EventMesh Dashboard

### Usage

```
cd ~/service
git clone -b dev https://github.com/apache/eventmesh-dashboard.git
cd eventmesh-dashboard
chmod +x deployment/auto-deploy-eventmesh-dashboard.sh
```

Edit credentials:

```
cp deployment/.env.example deployment/.env
vim deployment/.env
```

Add task to crontab:

```
crontab -e
```

```
0 * * * * ~/service/eventmesh-dashboard/deployment/auto-deploy-eventmesh-dashboard.sh
```