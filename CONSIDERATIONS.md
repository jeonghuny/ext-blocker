
-MIME 스푸핑 항목 근거-

kjh@kjhui-MacBookPro ext-blocker % curl -s -F "file=@test.txt;type=image/png" $BASE/api/probe/upload | jq .declaredContentType
# "image/png"
zsh: command not found: #