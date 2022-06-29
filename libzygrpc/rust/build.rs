use std::path::PathBuf;

fn main() {
    let out_dir = PathBuf::from("src");

    tonic_build::configure()
        .out_dir(out_dir)
        .compile(&["../proto/zhiyan_rpc.proto"],&["../"])
        .unwrap();
}
