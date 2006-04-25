#!/usr/bin/ruby -w

if ARGV.length() != 1
  print("usage: blogger-backup.rb <blog-name>\n")
  exit(1)
end

blog_name=ARGV[0]
blog_uri="http://#{blog_name}.blogspot.com/"

print("Downloading #{blog_uri}...\n")
blog_content=`curl --silent --show-error #{blog_uri}`

blog_content.split("\n").each() {
  |line|
  if line =~ /"(#{blog_uri}(\d+_\d+_\d+_\S+_archive\.html))"/
    next_archive_uri=$1
    next_filename=$2
    print(" page #{next_archive_uri}...\n")
    system("curl --silent --show-error -o #{next_filename} #{next_archive_uri}")
    File.new(next_filename).read().split("<").each() {
      |line|
      if line =~ /="(http:\/\/photos\d+\.blogger\.com\/[^"]+\/([^\/"]+\.(png|jpg)))"/
        image_uri = $1
        image_filename = $2
        print("      image #{image_uri}...\n")
        system("curl --silent --show-error -o #{image_filename} #{image_uri}")
      end
    }
  end
}
