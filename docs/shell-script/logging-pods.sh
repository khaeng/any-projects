#!/bin/bash

# 사용법 안내: OpenShift Pods 로그 실시간 모니터링
# ./logging-pods.sh <namespace> <pod-prefix>
# 예: ./logging-pods.sh ncp-fo-dev ncp-
# 다른 명령으로 변경하여 수행가능: oc --> kubectl

if [ $# -lt 2 ]; then
  echo ""
  echo "Usage: $0 <namespace> <pod-prefix>"
  echo ""
  echo "Example: $0 ncp-fo-dev ncp-"
  echo ""
  echo "=================== Namespace List startswith 'ncp-' ==================="
  oc get projects | grep ncp-
  echo ""

  if [ $# -eq 1 ]; then
    echo "=============== Pods List within Namespace[ $1 ] ==============="
    oc get pods -n $1
    echo ""
  fi
  exit 1
fi

NAMESPACE=$1
PREFIX=$2

# Ctrl + C 누르면 자식 프로세스 모두 종료
trap "echo 'Stopping...'; kill 0" SIGINT

### 변경전 로그 모니터링 방식 (1회만 조회하여 실행 후 변경을 감지 못함.)
# # Pod 목록 가져와서 로그 tail -f
# for pod in $(oc get pods -n $NAMESPACE | grep "^$PREFIX" | awk '{print $1}'); do
#   echo ">>> Logs from $pod"
#   oc logs -f $pod -n $NAMESPACE --tail=50 &
# done

### 변경되는 Pod들을 감시하여 로그를 실시간으로 모니터링.
# 감시중인 Pod들을 기록할 집합
declare -A WATCHED

while true; do
  # 현재 Pod 목록 가져오기
  for pod in $(oc get pods -n "$NAMESPACE" --no-headers | awk '{print $1}' | grep "^$PREFIX"); do
    if [[ -z "${WATCHED[$pod]}" ]]; then
      echo ">>> Start logging $pod"
      oc logs -f "$pod" -n "$NAMESPACE" --tail=50 &
      WATCHED[$pod]=1
    fi
  done
  sleep 5
done


wait

