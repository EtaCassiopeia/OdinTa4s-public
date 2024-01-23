# OdinTa4s

OdinTa4s is a technical analysis tool along with an algorithmic trading strategy and application

Note: This project is made public for educational and demo purposes only. It is not intended to be used as a trading bot.

------

# Publish to Google Cloud

## Create a Docker image

```shell
sbt docker:publishLocal
```

## Run the Docker image locally

```shell
docker run eu.gcr.io/odin-trader/trader:0.1.0-SNAPSHOT
```

## Push Docker images

```shell
gcloud auth configure-docker
docker push eu.gcr.io/odin-trader/trader:0.1.0-SNAPSHOT

gcloud container images list-tags eu.gcr.io/odin-trader/trader
```

## Create the VM instance

Create a ComputeEngine instance from a COS image (cos-69-lts) and specify the Docker image to start the VM instance (Don't forget to add the environment variables)


## Create the VM instance (using gcloud tool)
```shell
gcloud beta compute --project=odin-trader instances create-with-container odin-trader-trader --zone=europe-west4-b --machine-type=f1-micro --subnet=default --network-tier=PREMIUM --metadata=google-logging-enabled=true --maintenance-policy=MIGRATE --service-account=<SERVICE_ACCOUNT> --scopes=https://www.googleapis.com/auth/devstorage.read_only,https://www.googleapis.com/auth/logging.write,https://www.googleapis.com/auth/monitoring.write,https://www.googleapis.com/auth/servicecontrol,https://www.googleapis.com/auth/service.management.readonly,https://www.googleapis.com/auth/trace.append --tags=http-server,https-server --image=cos-stable-81-12871-103-0 --image-project=cos-cloud --boot-disk-size=10GB --boot-disk-type=pd-standard --boot-disk-device-name=odin-trader-api --no-shielded-secure-boot --shielded-vtpm --shielded-integrity-monitoring --container-image=eu.gcr.io/odin-trader/api:0.0.1 --container-restart-policy=always --container-env=SERVER_HOST=0.0.0.0,SERVER_PORT=80,BINANCE_SCHEME=https,BINANCE_HOST=api.binance.com,BINANCE_PORT=443,BINANCE_INFO_URL=/api/v1/exchangeInfo,BINANCE_API_KEY=<BINANCE_API_KEY>,BINANCE_API_SECRET=<BINANCE_API_SECRET> --labels=container-vm=cos-stable-81-12871-103-0 --reservation-affinity=any

gcloud compute --project=odin-trader firewall-rules create default-allow-https --direction=INGRESS --priority=1000 --network=default --action=ALLOW --rules=tcp:443 --source-ranges=0.0.0.0/0 --target-tags=https-server
```

## ssh

```shell
gcloud beta compute ssh --zone "europe-west4-b" "odin-trader-api" --project "odin-trader"
```

## Binance webhooks
Webhooks allow you to send a POST request to a certain URL every time the alert is triggered.
If the alert message is valid JSON, Binance will send a request with an "application/json" content-type header. Otherwise, it will send "text/plain" as a content-type header.

If you want to send a request to a URL with a port number, please note that we only accept URLs with port numbers 80 and 443. Requests for URLs with any other port number will be rejected.

## Binance IP addresses

Here is a list of IP addresses that Binance will use to send POST requests

- 52.89.214.238
- 34.212.75.30
- 54.218.53.128
- 52.32.178.7

## CIDR IP ranges

- 52.89.214.238/32
- 34.212.75.30/32
- 54.218.53.128/32
- 52.32.178.7/32