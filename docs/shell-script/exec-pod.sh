#!/bin/bash

# 사용법 안내: OpenShift Pods 로그 실시간 모니터링
# ./logging-pods.sh <namespace> <pod-prefix>
# 예: ./logging-pods.sh ncp-fo-dev ncp-
# 다른 명령으로 변경하여 수행가능: oc --> kubectl

if [ $# -lt 2 ]; then
  echo ""
  echo "Usage: $0 <namespace> <pod-name> <command>"
  echo ""
  echo "Example: $0 ncp-fo-dev ncp-fo-dev-00001 /bin/bash"
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
POD=$2
CMD=$3
if [ -z "$CMD" ]; then
  echo "Command not specified, defaulting to '/bin/sh'"
  CMD="/bin/sh"
fi

oc exec -it $POD -n $NAMESPACE -- $CMD
