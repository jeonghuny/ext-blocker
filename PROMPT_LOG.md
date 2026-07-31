
[측정1. 파일 크기 한계 — 플랫폼 천장 찾기]

kjh@kjhui-MacBookPro ext-blocker-probe % for s in 1 5 10 30 100; do
printf "%4sMB → " $s
curl -s -o /dev/null -w "%{http_code}  %{time_total}s\n" \
-F "file=@test_${s}mb.bin" $BASE/api/probe/upload
done
1MB → 200  0.292554s
5MB → 200  0.255023s
10MB → 200  0.338433s
30MB → 200  0.567074s
100MB → 200  0.298860s

kjh@kjhui-MacBookPro ext-blocker-probe % curl -s -F "file=@test_1mb.bin" $BASE/api/probe/upload > /dev/null
kjh@kjhui-MacBookPro ext-blocker-probe % curl -s $BASE/api/probe/status | jq '{tmpFileCount, tmpFiles}'
{
"tmpFileCount": 6,
"tmpFiles": [
"c8436b65-a275-400f-845e-b5154eff3c29.bin",
"eddda71e-0d59-40fe-9bef-a93f526ed48b.bin",
"5b3ee24e-ba4e-4119-9384-fca0aa5c8af6.bin",
"766fb0cd-0d60-483e-8e72-8f9f81fc2fda.bin",
"bbeedffc-5286-4249-8df8-f2e648a5dee6.bin",
"d032879f-0122-4a56-8da0-8f2441f47776.bin"
]
}

