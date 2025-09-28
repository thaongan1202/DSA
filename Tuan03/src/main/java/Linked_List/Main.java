package Linked_List;

public class Main {
    public static void main(String[] args) {
        LinkedList list = new LinkedList();

        //them phan tu
        list.addFirst(10);
        list.addFirst(20);
        list.addFirst(30);

        System.out.println("Danh sach:");
        list.printList();

        //tim kiem
        System.out.println("Vi tri cua so 10 la:" + list.search(10));

        //xoa phan tu
        System.out.println("Danh sach sau khi xoa phan tu dau tien la:");
        list.removeFirst();
        list.printList();

        //them phan tu dau tien
        System.out.println("Danh sach sau khi them 50 vao vi tri dau tien la:");
        list.addFirst(50);
        list.printList();

        System.out.println("Them vao vi tri 2 phan tu 100:");
        list.addAt(2, 100);
        list.printList();

    }
}
