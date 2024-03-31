## Auto Deploy EventMesh Dashboard

### Usage

```
cd ~/service
git clone -b dev https://github.com/apache/eventmesh-dashboard.git
cd eventmesh-dashboard/deployment/
```

Edit credentials:

```
cp .env.example .env
vim .env
```

Add task to crontab:

```
crontab -e
```

```
0 * * * * bash ~/service/eventmesh-dashboard/deployment/auto-deploy-eventmesh-dashboard.sh
```