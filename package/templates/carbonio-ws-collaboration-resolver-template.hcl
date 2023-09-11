Kind    = "service-resolver"
Name    = "carbonio-ws-collaboration"

Subsets = {
{{ range service "carbonio-ws-collaboration" }}
  {{ .ServiceMeta.ServiceId }} = {
    filter = "Service.Meta.ServiceId == {{ .ServiceMeta.ServiceId }}"
  }
{{ end }}
}