module other.service.impl {
  requires other.service.api;

  provides other.service.api.IOtherService with other.service.impl.OtherServiceImpl;
}
