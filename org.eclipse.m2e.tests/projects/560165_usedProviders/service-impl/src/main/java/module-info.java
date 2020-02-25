module service.impl {
  requires service.api;

  provides service.api.IService with service.impl.ServiceImpl;
}
